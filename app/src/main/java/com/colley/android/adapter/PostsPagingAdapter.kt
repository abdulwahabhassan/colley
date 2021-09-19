package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.model.Post
import com.colley.android.model.Profile
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.firebase.ui.database.paging.FirebaseRecyclerPagingAdapter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class PostsPagingAdapter(
    options: DatabasePagingOptions<Post>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: PostPagingItemClickedListener
) : FirebaseRecyclerPagingAdapter<Post, PostPagingViewHolder>(options) {

    interface PostPagingItemClickedListener {
        fun onItemClick(postId: String, view: View)
        fun onItemLongCLicked(postId: String, view: View)
        fun onUserClicked(userId: String, view: View)
        fun onCommentClicked(postId: String, view: View, viewHolder: PostPagingViewHolder)
        fun onLikeClicked(postId: String, view: View, viewHolder: PostPagingViewHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPagingViewHolder {
        val viewBinding = ItemPostBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PostPagingViewHolder(viewBinding)
    }

    override fun onBindViewHolder(viewHolder: PostPagingViewHolder, position: Int, model: Post) {
        viewHolder.bind(currentUser, model, context, clickListener, viewHolder)
    }

    //returns a unique view for each item
    override fun getItemViewType(position: Int): Int {
        return position
    }

}


class PostPagingViewHolder (val itemBinding : ItemPostBinding)
    : RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        currentUser: FirebaseUser?,
        post: Post,
        context: Context,
        clickListener: PostsPagingAdapter.PostPagingItemClickedListener,
        viewHolder: PostPagingViewHolder
    ) = with(itemBinding) {

        //check if userId is not null
        post.userId?.let { userId ->
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
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into(userPhotoImageView)
                    } else {
                        Glide.with(root.context).load(R.drawable.ic_person)
                            .into(userPhotoImageView)
                    }
                }
        }

        if(post.image != null) {
            contentImageView.visibility = VISIBLE
            Glide.with(root.context).load(post.image)
                .placeholder(R.drawable.ic_downloading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(contentImageView)

        } else {
            contentImageView.visibility = GONE
        }

        //set time stamp
        timeStampTextView.text = post.timeStamp

        //dismiss post text view if null else set it
        if(post.text == null) {
            contentTextView.visibility = GONE
        } else {
            contentTextView.visibility = VISIBLE
            contentTextView.text = post.text
        }

        //dismiss post location view if null else set it
        if(post.location == null) {
            locationTextView.visibility = GONE
        } else {
            locationTextView.visibility = VISIBLE
            locationTextView.text = post.location
        }

        //update likeTextView start drawable depending on the value
        //of liked
        post.postId?.let {
            Firebase.database.reference.child("posts").child(it).child("likes")
                .child(currentUser?.uid!!).get().addOnSuccessListener {
                    snapShot -> likeTextView.isActivated = snapShot.getValue(Boolean::class.java) == true
                }

        }

        //set likes count
        when (post.likesCount) {
            0 -> likeCountTextView.visibility = GONE
            1 -> {
                likeCountTextView.visibility = VISIBLE
                likeCountTextView.text = "${post.likesCount} like"
            }
            else -> {
                likeCountTextView.visibility = VISIBLE
                likeCountTextView.text = "${post.likesCount} likes"
            }
        }

        //set comments count
        when (post.commentsCount) {
            0 -> commentCountTextView.visibility = GONE
            1 -> {
                commentCountTextView.visibility = VISIBLE
                commentCountTextView.text = "${post.commentsCount} comment"
            }
            else -> {
                commentCountTextView.visibility = VISIBLE
                commentCountTextView.text = "${post.commentsCount} comments"
            }
        }

        //set promotions count
        when (post.promotionsCount) {
            0 -> promotionCountTextView.visibility = GONE
            1 -> {
                promotionCountTextView.visibility = VISIBLE
                promotionCountTextView.text = "${post.promotionsCount} promotion"
            }
            else -> {
                promotionCountTextView.visibility = VISIBLE
                promotionCountTextView.text = "${post.promotionsCount} promotions"
            }
        }

        root.setOnClickListener {

            if(post.postId != null) {
                clickListener.onItemClick(post.postId, it)
            }
        }

        root.setOnLongClickListener {
            if(post.postId != null) {
                clickListener.onItemLongCLicked(post.postId, it)
            }
            true
        }

        nameTextView.setOnClickListener {
            if(post.userId != null) {
                clickListener.onUserClicked(post.userId, it)
            }
        }

        userPhotoImageView.setOnClickListener {
            if(post.userId != null) {
                clickListener.onUserClicked(post.userId, it)
            }
        }

        commentLinearLayout.setOnClickListener {
            if(post.postId != null) {
                clickListener.onCommentClicked(post.postId, it, viewHolder)
            }
        }

        likeLinearLayout.setOnClickListener {
            if(post.postId != null) {
                clickListener.onLikeClicked(post.postId, it, viewHolder)
            }
        }
    }
}

