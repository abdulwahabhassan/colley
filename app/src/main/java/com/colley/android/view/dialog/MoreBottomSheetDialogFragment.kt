package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.colley.android.databinding.BottomSheetDialogFragmentMoreBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MoreBottomSheetDialogFragment (
    private val parentContext: Context,
    private val postView: View,
    private val moreOptionsDialogListener: MoreOptionsDialogListener
        ) :
    BottomSheetDialogFragment() {

    private var postId: String? = null
    private var _binding: BottomSheetDialogFragmentMoreBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid
    interface MoreOptionsDialogListener {
        fun onDeletePost(postId: String?)
        fun onReportPost(postId: String?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //retrieve post id from bundle
            postId = it.getString(POST_ID_KEY)
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
        _binding = BottomSheetDialogFragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            deletePost.setOnClickListener {
//                postId?.let { it1 -> dbRef.child("posts").child(it1).
                //}

                moreOptionsDialogListener.onDeletePost(postId)
            }

            reportPost.setOnClickListener {
                moreOptionsDialogListener.onReportPost(postId)
            }
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val POST_ID_KEY = "postIdKey"
    }


}