package com.colley.android.adapter

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.databinding.ItemSchoolBinding
import com.colley.android.model.Group
import com.colley.android.model.Post

class PostsFragmentRecyclerAdapter() : RecyclerView.Adapter<PostsFragmentRecyclerAdapter.PostViewHolder>() {

    var listOfPosts = arrayListOf<Post>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(post: Post)
    }

    class PostViewHolder (private val itemBinding : ItemPostBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(post: Post, clickListener: ItemClickedListener) = with(itemBinding) {

            nameTextView.text = post.name
            schoolTextView.text = post.school
            timeStampTextView.text = post.timeStamp
            contentTextView.text = post.text

            if(post.location == null) {
                locationTextView.visibility = GONE
            } else {
                locationTextView.text = post.location
            }

            when (post.likes) {
                0 -> likeCountTextView.visibility = GONE
                1 -> likeCountTextView.text = "${post.likes} like"
                else -> likeCountTextView.text = "${post.likes} likes"
            }

            when (post.comments) {
                0 -> commentCountTextView.visibility = GONE
                1 -> commentCountTextView.text = "${post.comments} comment"
                else -> commentCountTextView.text = "${post.comments} comments"
            }

            when (post.promotions) {
                0 -> promotionCountTextView.visibility = GONE
                1 -> promotionCountTextView.text = "${post.promotions} promotion"
                else -> promotionCountTextView.text = "${post.promotions} promotions"
            }

            Glide.with(this.root.context).load(post.image).into(contentImageView)
            Glide.with(this.root.context).load(post.userPhoto).into(userPhotoImageView)

            this.root.setOnClickListener {
                clickListener.onItemClick(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val viewBinding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = listOfPosts[position]
        holder.bind(post, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfPosts.size
    }

    fun setList(list: ArrayList<Post>, clickListener: ItemClickedListener) {
        this.listOfPosts = list
        this.clickListener = clickListener
        notifyDataSetChanged()
    }
}