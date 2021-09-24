package com.colley.android.view.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.colley.android.databinding.FragmentRaiseIssueBottomSheetDialogBinding
import com.colley.android.model.Issue
import com.colley.android.view.fragment.HomeFragmentDirections
import com.colley.android.view.fragment.IssuesFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class RaiseIssueBottomSheetDialogFragment(
    private val parentContext: Context,
    private val issuesView: View,
    private val newIssueListener: NewIssueListener,
    private val homeFabListener: RaiseIssueHomeFabListener
) : BottomSheetDialogFragment() {

    private var _binding: FragmentRaiseIssueBottomSheetDialogBinding? = null
    private val binding get() = _binding
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid

    interface NewIssueListener {
        fun navigateToIssue(issueId: String)
    }

    interface RaiseIssueHomeFabListener {
        fun enableFab(enabled: Boolean)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRaiseIssueBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        homeFabListener.enableFab(true)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        binding?.raiseIssueButton?.setOnClickListener {

            //Disable editing during creation
            setEditingEnabled(false)

            val title = binding?.issueTitleNameEditText?.text.toString().trim()
            val body = binding?.issueBodyEditText?.text.toString().trim()
            val df: DateFormat = SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss")
            val date: String = df.format(Calendar.getInstance().time)

            //if fields are empty, do not upload issue to database
            if (title == "" && body == "") {
                Toast.makeText(parentContext, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                setEditingEnabled(true)
                return@setOnClickListener
            }

            //make instance of new issue
            val issue = Issue(
                 userId = uid,
                 title = title,
                 body = body,
                 timeStamp = date,
             )

            //create and push new issue to database, retrieve key and add it as issueId
            dbRef.child("issues").push()
                .setValue(issue, DatabaseReference.CompletionListener { error, ref ->
                    homeFabListener.enableFab(false)
                    if (error != null) {
                        Toast.makeText(
                            parentContext,
                            "Unable to create issue",
                            Toast.LENGTH_LONG).show()

                        Log.w(
                            AddGroupBottomSheetDialogFragment.TAG,
                            "Unable to write issue to database. ${error.message}"
                        )

                        setEditingEnabled(true)
                        return@CompletionListener
                    }
                    //after creating issue, retrieve its key on the database and set it as the issue id
                    val key = ref.key
                    dbRef.child("issues").child(key!!).child("issueId").setValue(key).addOnCompleteListener {

                        //update issues count
                        dbRef.child("issuesCount").runTransaction(
                            object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    var count = currentData.getValue(Int::class.java)
                                    if (count != null){
                                        currentData.value = count++
                                    } else {
                                        count = 1
                                    }
                                    currentData.value =count
                                    //set database count value to the new update
                                    return Transaction.success(currentData)
                                }

                                override fun onComplete(
                                    error: DatabaseError?,
                                    committed: Boolean,
                                    currentData: DataSnapshot?
                                ) {
                                    if(error == null){
                                        //navigate to issue
                                        newIssueListener.navigateToIssue(key)
                                    }
                                }
                            }
                        )
                    }
                })

            Toast.makeText(
                parentContext,
                "New issue raised",
                Toast.LENGTH_LONG).show()
            this.dismiss()
        }
    }

    //used to disable fields during creation
    private fun setEditingEnabled(enabled: Boolean) {
            binding?.issueTitleNameEditText?.isEnabled = enabled
            binding?.issueBodyEditText?.isEnabled = enabled
            binding?.raiseIssueButton?.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
