package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.model.Post
import com.colley.android.model.Profile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class PostsPagingAdapter (
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: PostPagingItemClickedListener,
) : PagingDataAdapter<DataSnapshot, PostViewHolder>(POST_COMPARATOR) {

    interface PostPagingItemClickedListener {
        fun onItemClick(postId: String, view: View, viewHolder: PostViewHolder)
        fun onItemLongCLicked(postId: String, view: View, viewHolder: PostViewHolder)
        fun onUserClicked(userId: String, view: View)
        fun onCommentClicked(postId: String, view: View, viewHolder: PostViewHolder)
        fun onLikeClicked(postId: String, view: View, viewHolder: PostViewHolder)
        fun onSaveClicked(postId: String, it: View, viewHolder: PostViewHolder)
        fun onMoreClicked(postId: String, it: View?, viewHolder: PostViewHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val viewBinding = ItemPostBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(viewBinding)
    }
    override fun onBindViewHolder(viewHolder: PostViewHolder, position: Int) {
        viewHolder.bind(currentUser, getItem(position), context, clickListener, viewHolder)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    companion object {
        private val POST_COMPARATOR = object : DiffUtil.ItemCallback<DataSnapshot>() {
            override fun areItemsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(Post::class.java)?.postId == newItem.getValue(Post::class.java)?.postId
            }

            override fun areContentsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(Post::class.java) == newItem.getValue(Post::class.java)
            }

        }
    }
}

class PostViewHolder (val itemBinding : ItemPostBinding)
    : RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        currentUser: FirebaseUser?,
        dataSnapshot: DataSnapshot?,
        context: Context,
        clickListener: PostsPagingAdapter.PostPagingItemClickedListener,
        viewHolder: PostViewHolder
    ) = with(itemBinding) {

        val post = dataSnapshot?.getValue(Post::class.java)

        //check if userId is not null
        post?.userId?.let { userId ->
            Firebase.database.reference.child("profiles").child(userId).get().addOnSuccessListener {
                    snapShot ->
                val profile = snapShot.getValue(Profile::class.java)
                if(profile != null) {
                    //set user name and school and make views visible
                    nameTextView.text = profile.name
                    schoolTextView.text = profile.school
                }
            }

            //retrieve user photo
            Firebase.database.reference.child("photos").child(userId).get().addOnSuccessListener {
                    snapShot ->
                val photo = snapShot.getValue(String::class.java)
                //set photo
                if (photo != null) {
                    Glide.with(root.context).load(photo)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(userPhotoImageView)
                } else {
                    Glide.with(root.context).load(R.drawable.ic_person).into(userPhotoImageView)
                }
            }
        }

        if(post?.image != null) {
            contentImageView.visibility = View.VISIBLE
            Glide.with(root.context).load(post.image)
                .placeholder(R.drawable.ic_downloading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(contentImageView)

        } else {
            contentImageView.visibility = View.GONE
        }

        //set time stamp
        timeStampTextView.text = post?.timeStamp

        //dismiss post text view if null else set it
        if(post?.text == null) {
            contentTextView.visibility = View.GONE
        } else {
            contentTextView.visibility = View.VISIBLE
            contentTextView.text = post.text
        }

        //dismiss post location view if null else set it
        if(post?.location == null) {
            locationTextView.visibility = View.GONE
        } else {
            locationTextView.visibility = View.VISIBLE
            locationTextView.text = post.location
        }

        //update likeTextView start drawable depending on the value
        //of liked
        post?.postId?.let {
            Firebase.database.reference.child("post-likes").child(it)
                .child(currentUser?.uid!!).get().addOnSuccessListener {
                        snapShot -> likeTextView.isActivated = snapShot.getValue(Boolean::class.java) == true
                }

        }

        //update savePostTextView start drawable based on whether user has saved this post or not
        post?.postId?.let { postId ->
            Firebase.database.reference.child("user-saved_posts")
                .child(currentUser?.uid!!).get().addOnSuccessListener { dataSnapshot ->
                    savePostTextView.isActivated =
                        dataSnapshot.getValue<ArrayList<String>>()?.contains(postId) == true

                }
        }

        //set likes count
        when (post?.likesCount) {
            0 -> likeCountTextView.visibility = View.GONE
            1 -> {
                likeCountTextView.visibility = View.VISIBLE
                likeCountTextView.text = "${post.likesCount} like"
            }
            else -> {
                likeCountTextView.visibility = View.VISIBLE
                likeCountTextView.text = "${post?.likesCount} likes"
            }
        }

        //set comments count
        when (post?.commentsCount) {
            0 -> commentCountTextView.visibility = View.GONE
            1 -> {
                commentCountTextView.visibility = View.VISIBLE
                commentCountTextView.text = "${post.commentsCount} comment"
            }
            else -> {
                commentCountTextView.visibility = View.VISIBLE
                commentCountTextView.text = "${post?.commentsCount} comments"
            }
        }

        //click post to show post interactions (comments, likes and promotions)
        root.setOnClickListener {
            Log.w("clickListener", "${post?.postId}")
            if(post?.postId != null) {
                Log.w("clickListener", "${post?.postId}")
                clickListener.onItemClick(post.postId, it, viewHolder)
            }
        }


        root.setOnLongClickListener {
            if(post?.postId != null) {
                clickListener.onItemLongCLicked(post.postId, it, viewHolder)
            }
            true
        }

        //click user name to show profile
        nameTextView.setOnClickListener {
            if(post?.userId != null) {
                clickListener.onUserClicked(post.userId, it)
            }
        }

        //click user photo to show profile
        userPhotoImageView.setOnClickListener {
            if(post?.userId != null) {
                clickListener.onUserClicked(post.userId, it)
            }
        }

        //click comment to show comment dialog to comment on post
        commentLinearLayout.setOnClickListener {
            if(post?.postId != null) {
                clickListener.onCommentClicked(post.postId, it, viewHolder)
            }
        }

        //click like to show dialog to like post
        likeLinearLayout.setOnClickListener {
            if(post?.postId != null) {
                clickListener.onLikeClicked(post.postId, it, viewHolder)
            }
        }

        //click save to save post
        savePostLinearLayout.setOnClickListener {
            if(post?.postId != null) {
                clickListener.onSaveClicked(post.postId, it, viewHolder)
            }
        }

        moreImageView.setOnClickListener {
            if(post?.postId != null) {
                clickListener.onMoreClicked(post.postId, it, viewHolder)
            }
        }
    }
}



