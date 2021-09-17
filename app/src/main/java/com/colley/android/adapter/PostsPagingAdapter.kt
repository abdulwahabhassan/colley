package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
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
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.firebase.ui.database.paging.FirebaseRecyclerPagingAdapter
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PostsPagingAdapterExp(
    options: DatabasePagingOptions<Post>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: PostPagingItemClickedListener
) : FirebaseRecyclerPagingAdapter<Post, PostPagingViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPagingViewHolder {
        val viewBinding = ItemPostBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PostPagingViewHolder(viewBinding)
    }

    override fun onBindViewHolder(viewHolder: PostPagingViewHolder, position: Int, model: Post) {
        viewHolder.bind(currentUser, model, context, clickListener)
    }

    interface PostPagingItemClickedListener {
        fun onItemClick(postId: String, view: View)
        fun onItemLongCLicked(postId: String, view: View)
        fun onUserClicked(userId: String, view: View)
    }

}


class PostPagingViewHolder (private val itemBinding : ItemPostBinding)
    : RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        currentUser: FirebaseUser?,
        post: Post, context: Context,
        clickListener: PostsPagingAdapterExp.PostPagingItemClickedListener) = with(itemBinding) {

        //check if userId is not null
        post.userId?.let { userId ->
                Firebase.database.reference.child("profiles").child(userId)
                    .addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val profile = snapshot.getValue<Profile>()
                            if(profile != null) {
                                //set user name and school and make views visible
                                nameTextView.text = profile.name
                                schoolTextView.text = profile.school
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }
                )

                //retrieve user photo
                Firebase.database.reference.child("photos").child(userId)
                    .addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val photo = snapshot.getValue<String>()
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

                        override fun onCancelled(error: DatabaseError) {}
                    }
                )
        }

        if(post.image != null) {
            contentImageView.visibility = VISIBLE
            Glide.with(root.context).load(post.image).centerCrop()
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

        //set likes count
        when (post.likes) {
            0 -> likeCountTextView.visibility = GONE
            1 -> {
                likeCountTextView.visibility = VISIBLE
                likeCountTextView.text = "${post.likes} like"
            }
            else -> {
                likeCountTextView.visibility = VISIBLE
                likeCountTextView.text = "${post.likes} likes"
            }
        }

        //set comments count
        when (post.comments) {
            0 -> commentCountTextView.visibility = GONE
            1 -> {
                commentCountTextView.visibility = VISIBLE
                commentCountTextView.text = "${post.comments} comment"
            }
            else -> {
                commentCountTextView.visibility = VISIBLE
                commentCountTextView.text = "${post.comments} comments"
            }
        }

        //set promotions count
        when (post.promotions) {
            0 -> promotionCountTextView.visibility = GONE
            1 -> {
                promotionCountTextView.visibility = VISIBLE
                promotionCountTextView.text = "${post.promotions} promotion"
            }
            else -> {
                promotionCountTextView.visibility = VISIBLE
                promotionCountTextView.text = "${post.promotions} promotions"
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
    }
}

