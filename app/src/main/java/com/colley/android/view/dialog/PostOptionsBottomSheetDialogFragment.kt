package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import com.colley.android.databinding.BottomSheetDialogFragmentMoreBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PostOptionsBottomSheetDialogFragment (
    private val parentContext: Context,
    private val postView: View,
    private val moreOptionsDialogListener: MoreOptionsDialogListener
        ) :
    BottomSheetDialogFragment() {

    private var postUserId: String? = null
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
            //retrieve post id from bundle and the id of the user that made the post
            postId = it.getString(POST_ID_KEY)
            postUserId = it.getString(POST_USER_ID_KEY)
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

            //if the current user is the same user who made the post,
            //make delete post TextView visible
            if(uid == postUserId) {
                deletePost.visibility = VISIBLE
            } else {
                deletePost.visibility = GONE
            }

            deletePost.setOnClickListener {
                //prevent multiple clicks
                deletePost.isEnabled = false
                postId?.let { postId ->
                    //get post image url if any exists
                    dbRef.child("posts").child(postId).child("image").get()
                        .addOnSuccessListener { snapShot ->
                        val imageUrl = snapShot.getValue(String::class.java)
                        //if this post has an image
                        if (imageUrl != null) {
                            //get a reference to it's location on database storage from its url and
                            //delete it with the retrieved reference
                            Firebase.storage.getReferenceFromUrl(imageUrl).delete()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            parentContext,
                                            "Deleted media",
                                            Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(
                                            parentContext,
                                            "Failed to delete media",
                                            Toast.LENGTH_LONG).show()
                                        }
                                }
                        }
                    }
                    //delete post from database, pass a DatabaseReference.onCompletionListener
                    dbRef.child("posts").child(postId).setValue(null) { error, ref ->
                        //if no error, continue further operations
                        if(error == null) {
                            //decrease posts count
                            dbRef.child("postsCount").runTransaction(
                                object : Transaction.Handler {
                                    override fun doTransaction(currentData: MutableData): Transaction.Result {
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

                            //set post likes to null
                            dbRef.child("post-likes").child(postId).setValue(null)

                            //set post comments to null
                            dbRef.child("post-comments").child(postId).setValue(null)

                            moreOptionsDialogListener.onDeletePost(postId)

                        }
                        this@PostOptionsBottomSheetDialogFragment.dismiss()
                    }

                }


            }

            reportPost.setOnClickListener {
                //prevent multiple clicks
                reportPost.isEnabled = false
                moreOptionsDialogListener.onReportPost(postId)
                this@PostOptionsBottomSheetDialogFragment.dismiss()
            }
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val POST_ID_KEY = "postIdKey"
        private const val POST_USER_ID_KEY = "userIdKey"
    }


}