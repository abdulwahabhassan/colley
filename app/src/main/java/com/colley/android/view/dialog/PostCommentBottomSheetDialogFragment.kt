package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.colley.android.databinding.FragmentPostCommentDialogBinding
import com.colley.android.model.Comment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class PostCommentBottomSheetDialogFragment (
    private val parentContext: Context,
    private val postView: View,
    private val commentListener: CommentListener
) : BottomSheetDialogFragment() {

    private var postId: String? = null
    private var _binding: FragmentPostCommentDialogBinding? = null
    private val binding get() = _binding
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid
    interface CommentListener {
        fun onComment(currentData: DataSnapshot?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //retrieve post id from issue fragment
            postId = it.getString(POST_ID_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostCommentDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        binding?.sendButton?.setOnClickListener {
            setEditingEnabled(false)

            val commentText = binding?.editCommentEditText?.text?.toString()?.trim()
            if (commentText != "") {

                //get current time and format it
                val df: DateFormat = SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss")
                val date: String = df.format(Calendar.getInstance().time)

                val comment = Comment(
                    commentText = commentText!!,
                    commentTimeStamp = date,
                    commenterId = uid
                )
                postId?.let { postId ->
                    //create and write new comment to database, retrieve key and add it as commentId
                    dbRef.child("posts").child(postId).child("comments").push().setValue(
                        comment, DatabaseReference.CompletionListener { error, ref ->
                            if (error != null) {
                                Toast.makeText(
                                    parentContext,
                                    "Unable to write comment to database",
                                    Toast.LENGTH_SHORT
                                ).show()

                                setEditingEnabled(true)
                                return@CompletionListener
                            }
                            //after writing comment to database, retrieve its key on the database and set it as the comment id
                            val key = ref.key
                            dbRef.child("posts").child(postId).child("comments")
                                .child(key!!).child("commentId").setValue(key)

                            //update comments count
                            dbRef.child("posts").child(postId).child("commentsCount")
                                .runTransaction(
                                    object : Transaction.Handler {
                                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                                            //retrieve the current value of count at this location
                                            var count = currentData.getValue<Int>()
                                            if (count != null) {
                                                count++
                                                currentData.value = count
                                            }
                                            //set database count value to the new update
                                            return Transaction.success(currentData)
                                        }

                                        override fun onComplete(
                                            error: DatabaseError?,
                                            committed: Boolean,
                                            currentData: DataSnapshot?
                                        ) {
                                            commentListener.onComment(currentData)
                                        }

                                    }
                                )
                        }
                    )
                    Toast.makeText(parentContext, "Commented", Toast.LENGTH_SHORT).show()
                    //dismiss dialog
                    this.dismiss()
                }
            } else {
                Toast.makeText(parentContext, "Can't send an empty comment", Toast.LENGTH_LONG)
                    .show()
                setEditingEnabled(true)
            }
        }
    }

    //used to disable fields during upload to database
    private fun setEditingEnabled(enabled: Boolean) {
        binding?.editCommentEditText?.isEnabled = enabled
        binding?.sendButton?.isEnabled = enabled
    }

    companion object {
        private const val POST_ID_KEY = "postIdKey"
    }
}