package com.colley.android.view.dialog

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.colley.android.databinding.BottomSheetDialogFragmentIssueOptionsBinding
import com.colley.android.databinding.BottomSheetDialogFragmentMoreBinding
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class IssueOptionsBottomSheetDialogFragment (
    private val parentContext: Context,
    private val issueView: View,
    private val moreOptionsDialogListener: IssueOptionsDialogListener
        ) :
    BottomSheetDialogFragment() {

    private var issueUserId: String? = null
    private var issueId: String? = null
    private var _binding: BottomSheetDialogFragmentIssueOptionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid
    interface IssueOptionsDialogListener {
        fun onDeleteIssue(issueId: String?)
        fun onReportIssue(issueId: String?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //retrieve post id from bundle and the id of the user that made the post
            issueId = it.getString(ISSUE_ID_KEY)
            issueUserId = it.getString(ISSUE_USER_ID_KEY)
        }
        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetDialogFragmentIssueOptionsBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {

            //if the current user is the same user who made the post,
            //make delete post TextView visible
            if(uid == issueUserId) {
                deleteIssue.visibility = VISIBLE
            } else {
                deleteIssue.visibility = GONE
            }

            deleteIssue.setOnClickListener {

                AlertDialog.Builder(parentContext)
                    .setMessage("Are you sure you want to delete this issue?")
                    .setPositiveButton("Yes") { dialog, which ->
                        issueId?.let { issueId ->
                            //delete issue from database, pass a DatabaseReference.onCompletionListener
                            dbRef.child("issues").child(issueId)
                                .setValue(null) { error, ref ->
                                //if no error, continue further operations
                                if(error == null) {
                                    //decrease issues count
                                    dbRef.child("issuesCount").runTransaction(
                                        object : Transaction.Handler {
                                            override fun doTransaction(currentData: MutableData):
                                                    Transaction.Result {
                                                var count = currentData.getValue(Int::class.java)
                                                if (count != null){
                                                    currentData.value = count--
                                                }
                                                currentData.value = count
                                                //set database count value to the new update
                                                return Transaction.success(currentData)
                                            }

                                            override fun onComplete(
                                                error: DatabaseError?,
                                                committed: Boolean,
                                                currentData: DataSnapshot?
                                            ) {}

                                        }
                                    )

                                    //set issue comments to null
                                    dbRef.child("issue-comments").child(issueId)
                                        .setValue(null)

                                    //trigger listeners
                                    moreOptionsDialogListener.onDeleteIssue(issueId)

                                }
                                this@IssueOptionsBottomSheetDialogFragment.dismiss()
                            }

                        }
                        //dismiss alert dialog
                        dialog.dismiss()

                    }.setNegativeButton("No") { dialog, which ->
                        dialog.dismiss()
                    }.show()



            }

            reportIssue.setOnClickListener {
                moreOptionsDialogListener.onReportIssue(issueId)
                this@IssueOptionsBottomSheetDialogFragment.dismiss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val ISSUE_ID_KEY = "issueIdKey"
        private const val ISSUE_USER_ID_KEY = "userIdKey"
    }


}