package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.colley.android.R
import com.colley.android.databinding.FragmentViewIssueBinding
import com.colley.android.databinding.FragmentViewPostBinding
import com.colley.android.glide.GlideImageLoader
import com.colley.android.model.Notification
import com.colley.android.model.Post
import com.colley.android.model.Profile
import com.colley.android.view.dialog.CommentOnPostBottomSheetDialogFragment
import com.colley.android.view.dialog.PostBottomSheetDialogFragment
import com.colley.android.view.dialog.PostOptionsBottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ViewPostFragment : Fragment(),
    CommentOnPostBottomSheetDialogFragment.CommentListener,
    PostOptionsBottomSheetDialogFragment.MoreOptionsDialogListener,
    PostBottomSheetDialogFragment.ActionsDialogListener {

    private val args: ViewPostFragmentArgs by navArgs()
    private var _binding: FragmentViewPostBinding? = null
    private var postUserId: String? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid
    private lateinit var postDialog: PostBottomSheetDialogFragment
    private lateinit var postOptionsOptionsDialog: PostOptionsBottomSheetDialogFragment
    private lateinit var sheetDialogCommentOn: CommentOnPostBottomSheetDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //Inflate the layout for this fragment
        _binding = FragmentViewPostBinding.inflate(inflater, container, false)
        return binding?.root

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef.child("posts").child(args.postId).get().addOnSuccessListener {
            dataSnapshot ->
            val post = dataSnapshot?.getValue(Post::class.java)

            //check if userId is not null
            post?.userId?.let { userId ->
                Firebase.database.reference.child("profiles").child(userId).get()
                    .addOnSuccessListener { snapShot ->
                    val profile = snapShot.getValue(Profile::class.java)
                    if(profile != null) {
                        //set user name and school and make views visible
                        binding.nameTextView.text = profile.name
                        binding.schoolTextView.text = profile.school
                    }
                }

                //retrieve user photo
                Firebase.database.reference.child("photos").child(userId).get()
                    .addOnSuccessListener {
                        snapShot ->
                    val photo = snapShot.getValue(String::class.java)
                    //set photo
                    if (photo != null) {
                        Glide.with(requireContext()).load(photo)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into(binding.userPhotoImageView)
                    } else {
                        Glide.with(requireContext()).load(R.drawable.ic_person)
                            .into(binding.userPhotoImageView)
                    }
                }
            }

            if(post?.image != null) {
                val options = RequestOptions()
                    .error(R.drawable.ic_downloading)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                binding.contentImageView.visibility = View.VISIBLE
                //using custom glide image loader to indicate progress in time
                GlideImageLoader(binding.contentImageView, binding.progressBar)
                    .load(post.image, options);

            } else {
                binding.contentImageView.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
            }

            //set time stamp
            binding.timeStampTextView.text = post?.timeStamp

            //dismiss post text view if null else set it
            if(post?.text == null) {
                binding.contentTextView.visibility = View.GONE
            } else {
                binding.contentTextView.visibility = View.VISIBLE
                binding.contentTextView.text = post.text
            }

            //dismiss post location view if null else set it
            if(post?.location == null) {
                binding.locationTextView.visibility = View.GONE
            } else {
                binding.locationTextView.visibility = View.VISIBLE
                binding.locationTextView.text = post.location
            }

            //update likeTextView start drawable depending on the value
            //of liked
            post?.postId?.let {
                Firebase.database.reference.child("post-likes").child(it)
                    .child(uid).get().addOnSuccessListener {
                            snapShot -> binding.likeTextView.isActivated =
                        snapShot.getValue(Boolean::class.java) == true
                    }

            }

            //update savePostTextView start drawable based on whether user has saved this post or not
            post?.postId?.let { postId ->
                Firebase.database.reference.child("user-saved_posts")
                    .child(uid).get().addOnSuccessListener { dataSnapshot ->
                        binding.savePostTextView.isActivated =
                            dataSnapshot.getValue<ArrayList<String>>()?.contains(postId) == true

                    }
            }

            //set likes count
            when (post?.likesCount) {
                0 -> binding.likeCountTextView.visibility = View.GONE
                1 -> {
                    binding.likeCountTextView.visibility = View.VISIBLE
                    binding.likeCountTextView.text = "${post.likesCount} like"
                }
                else -> {
                    binding.likeCountTextView.visibility = View.VISIBLE
                    binding.likeCountTextView.text = "${post?.likesCount} likes"
                }
            }

            //set comments count
            when (post?.commentsCount) {
                0 -> binding.commentCountTextView.visibility = View.GONE
                1 -> {
                    binding.commentCountTextView.visibility = View.VISIBLE
                    binding.commentCountTextView.text = "${post.commentsCount} comment"
                }
                else -> {
                    binding.commentCountTextView.visibility = View.VISIBLE
                    binding.commentCountTextView.text = "${post?.commentsCount} comments"
                }
            }

            //click post to show post interactions (comments, likes and promotions)
            binding.constraintLayout.setOnClickListener {
                Log.w("clickListener", "${post?.postId}")
                if(post?.postId != null && post.userId != null) {
                    postDialog = PostBottomSheetDialogFragment(
                        requireContext(),
                        requireView(),
                        this
                    )
                    //put post id and post owner id in bundle
                    postDialog.arguments = bundleOf(
                        "postIdKey" to args.postId,
                        "postUserIdKey" to postUserId)
                    postDialog.show(parentFragmentManager, null)
                }
            }


            //click comment to show comment dialog to comment on post
            binding.commentLinearLayout.setOnClickListener {
                if(post?.postId != null && post.userId != null) {
                    sheetDialogCommentOn = CommentOnPostBottomSheetDialogFragment(
                        requireContext(),
                        requireView(),
                        this
                    )
                    sheetDialogCommentOn.arguments = bundleOf(
                        "postIdKey" to args.postId,
                        "postUserIdKey" to postUserId)
                    sheetDialogCommentOn.show(parentFragmentManager, null)
                }
            }

            //click like to show dialog to like post
            binding.likeLinearLayout.setOnClickListener {
                if(post?.postId != null && post.userId != null) {
                    //Register like on database
                    dbRef.child("post-likes").child(args.postId).child(uid)
                        .runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                //retrieve the current value of like at this location
                                var liked = currentData.getValue<Boolean>()
                                //compute new value of liked
                                liked = liked == null || liked == false
                                //if false set value at location to null else true
                                if (liked == false) {
                                    currentData.value = null
                                } else {
                                    currentData.value = true
                                }
                                //set database liked value to the new update
                                return Transaction.success(currentData)
                            }

                            //on successful entry, update likes count
                            @SuppressLint("SimpleDateFormat")
                            override fun onComplete(
                                error: DatabaseError?,
                                committed: Boolean,
                                currentData: DataSnapshot?
                            ) {
                                //get current value of liked
                                val liked = currentData?.getValue(Boolean::class.java)
                                if (error != null) {
                                    Toast.makeText(
                                        context,
                                        "Unable to write like to database",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    //only create notification if post was liked not if post was unliked
                                    //and if itemActor(user liking the post) is not the same user that owns the post
                                    if (liked == true && postUserId != uid) {
                                        //notify the user who owns the post that a like was given on their
                                        //post
                                        postUserId?.let { postUserId ->

                                            //get current time and format it
                                            //timeId will be used for sorting notification from the most recent
                                            val df: DateFormat =
                                                SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss")
                                            val date: String = df.format(Calendar.getInstance().time)
                                            val timeId = SimpleDateFormat("yyyyMMddHHmmss")
                                                .format(Calendar.getInstance().time).toLong() * -1

                                            //create instance of notification
                                            val notification = Notification(
                                                itemId = args.postId,
                                                itemOwnerUserId = postUserId,
                                                itemActorUserId = uid,
                                                timeId = timeId,
                                                timeStamp = date,
                                                itemActionId = null,
                                                itemType = "post",
                                                itemActionType = "like"
                                            )

                                            //push notification, retrieve key and set as notification id
                                            dbRef.child("user-notifications").child(postUserId)
                                                .push().setValue(notification) { error, ref ->
                                                    if (error == null) {
                                                        val notificationKey = ref.key
                                                        dbRef.child("user-notifications")
                                                            .child(postUserId).child(notificationKey!!)
                                                            .child("notificationId")
                                                            .setValue(notificationKey)
                                                    }
                                                }
                                        }
                                    }
                                    //update likes count
                                    dbRef.child("posts").child(args.postId)
                                        .child("likesCount")
                                        .runTransaction(
                                            object : Transaction.Handler {
                                                override fun doTransaction(currentData: MutableData):
                                                        Transaction.Result {
                                                    //retrieve the current value of count at this location
                                                    var count = currentData.getValue<Int>()
                                                    if (count == null) {
                                                        count = 1
                                                    }
                                                    //if liked, increase count by 1 else decrease by 1
                                                    if (liked == true) {
                                                        count++
                                                    } else {
                                                        count--
                                                    }
                                                    currentData.value = count
                                                    //set database count value to the new update
                                                    return Transaction.success(currentData)
                                                }
                                                //after successfully updating likes count on database,
                                                //update ui
                                                override fun onComplete(
                                                    error: DatabaseError?,
                                                    committed: Boolean,
                                                    currentData: DataSnapshot?
                                                ) {
                                                    updateLikeCountTextView(currentData, liked)
                                                }
                                            }
                                        )
                                }
                            }

                        })
                }
            }

            //click save to save post
            binding.savePostLinearLayout.setOnClickListener {
                if(post?.postId != null) {
                    //register save to user's list of saved posts on database
                    dbRef.child("user-saved_posts").child(uid)
                        .runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                //retrieve the current list of saved posts at this location
                                var savedPosts = currentData.getValue<ArrayList<String>>()
                                //if no list is found and null is returned, create a new list
                                if (savedPosts == null) {
                                    savedPosts = arrayListOf()
                                }
                                //if list does not already contain post id, add it else remove it
                                if (!savedPosts.contains(args.postId)) {
                                    savedPosts.add(args.postId)
                                } else {
                                    savedPosts.remove(args.postId)
                                }
                                currentData.value = savedPosts
                                return Transaction.success(currentData)
                            }

                            override fun onComplete(
                                error: DatabaseError?,
                                committed: Boolean,
                                currentData: DataSnapshot?
                            ) {
                                if (error == null) {
                                    //get the updated list
                                    val updatedList = currentData?.getValue<ArrayList<String>>()
                                    //if it contains postid, toast saved
                                    if (updatedList?.contains(args.postId) == true) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Saved",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                        //update savedPostTextView start drawable icon
                                        binding.savePostTextView.isActivated = true
                                    } else {
                                        //Toast unsaved
                                        Toast.makeText(
                                            requireContext(),
                                            "UnSaved",
                                            Toast.LENGTH_SHORT)
                                            .show()
                                        //update savedPostTextView start drawable icon
                                        binding.savePostTextView.isActivated = false
                                    }
                                }
                            }
                        })
                }
            }

            //click to view more options for post
            binding.moreImageView.setOnClickListener {
                if(post?.postId != null) {
                    postOptionsOptionsDialog = PostOptionsBottomSheetDialogFragment(
                        requireContext(),
                        requireView(),
                        this
                    )
                    postOptionsOptionsDialog.arguments =
                        bundleOf("postIdKey" to args.postId, "userIdKey" to postUserId)
                    postOptionsOptionsDialog.show(parentFragmentManager, null)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLikeCountTextView(currentData: DataSnapshot?, liked: Boolean?) {
        when (currentData?.getValue(Int::class.java)) {
            0 -> binding.likeCountTextView.visibility = View.GONE
            1 -> {
                binding.likeCountTextView.visibility = View.VISIBLE
                binding.likeCountTextView.text =
                    "${currentData.getValue(Int::class.java).toString()} like"
            }
            else -> {
                binding.likeCountTextView.visibility = View.VISIBLE
                binding.likeCountTextView.text =
                    "${currentData?.getValue(Int::class.java).toString()} likes"

            }
        }
        //update likeTextView start drawable depending on the value
        //of liked
        binding.likeTextView.isActivated =
            !(liked == null || liked == false)
    }

    //update ui to display updated comment count
    //interface from comment listener
    override fun onComment(currentData: DataSnapshot?) {
        updateCommentCountTextView(currentData)
    }

    @SuppressLint("SetTextI18n")
    private fun updateCommentCountTextView(currentData: DataSnapshot?) {
        when (currentData?.getValue(Int::class.java)) {
            0 -> binding.commentCountTextView.visibility = View.GONE
            1 -> {
                binding.commentCountTextView.visibility = View.VISIBLE
                binding.commentCountTextView.text =
                    "${currentData.getValue(Int::class.java).toString()} comment"
            }
            else -> {
                binding.commentCountTextView.visibility = View.VISIBLE
                binding.commentCountTextView.text =
                    "${currentData?.getValue(Int::class.java).toString()} comments"
            }
        }
    }

    //update ui to display updated comments count
    //interface from post dialog listener
    override fun onCommented(currentData: DataSnapshot?) {
        updateCommentCountTextView(currentData)
    }

    //update ui to display updated like count
    //interface from post dialog listener
    override fun onLiked(currentData: DataSnapshot?, liked: Boolean?) {
        updateLikeCountTextView(currentData, liked)
    }

    //navigate back on delete post
    override fun onDeletePost(postId: String?) {
        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onReportPost(postId: String?) {
        AlertDialog.Builder(requireContext())
            .setMessage("This post has been flagged and will be reviewed")
            .setNegativeButton("Ok, dismiss") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}