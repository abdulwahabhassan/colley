package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.ItemLikeBinding
import com.colley.android.model.Profile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PostLikesPagingAdapter (
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: PostLikeItemClickedListener,
        ) : PagingDataAdapter<DataSnapshot, PostLikeViewHolder>(COMMENT_COMPARATOR) {

    interface PostLikeItemClickedListener {
        fun onItemClick(userId: String, view: View)
        fun onUserClicked(userId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostLikeViewHolder {
        val viewBinding = ItemLikeBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PostLikeViewHolder(viewBinding)
    }

    override fun onBindViewHolder(viewHolder: PostLikeViewHolder, position: Int) {
        viewHolder.bind(currentUser, getItem(position), context, clickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    companion object {
        private val COMMENT_COMPARATOR = object : DiffUtil.ItemCallback<DataSnapshot>() {
            override fun areItemsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(Boolean::class.java) == newItem.getValue(Boolean::class.java)
            }

        }
    }
}

class PostLikeViewHolder(private val itemBinding: ItemLikeBinding)
    : RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(
        currentUser: FirebaseUser?,
        dataSnapshot: DataSnapshot?,
        context: Context,
        clickListener: PostLikesPagingAdapter.PostLikeItemClickedListener) = with (itemBinding) {

        //retrieve snapshot's key (userId)
        val userId = dataSnapshot?.key

        if (userId != null) {

            //set user's name
            Firebase.database.reference.child("profiles").child(userId).get()
                .addOnSuccessListener { snapShot ->
                    val profile = snapShot.getValue(Profile::class.java)
                    //check if profile name is not null before setting name
                    if(profile != null) {
                        userNameTextView.text = profile.name
                    } else {
                        userNameTextView.text = context.getString(R.string.anonymous_text)
                    }
                }

            //load user's photo
            Firebase.database.reference.child("photos").child(userId).get()
                .addOnSuccessListener { snapShot ->
                    val photoUrl = snapShot.getValue(String::class.java)
                    //load photoUrl to view
                    if (photoUrl != null) {
                        Glide.with(context).load(photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(userImageView)
                    } else {
                        Glide.with(context).load(R.drawable.ic_person_light_pearl)
                            .into(userImageView)
                    }
                }

            //on click
            root.setOnClickListener {
                clickListener.onItemClick(userId, it)
            }

            userImageView.setOnClickListener {
                clickListener.onUserClicked(userId, it)
            }
        }
    }
}