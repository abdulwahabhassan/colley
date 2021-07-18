package com.colley.android.adapter

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.R.*
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.databinding.ItemSchoolBinding
import com.colley.android.model.Group
import com.colley.android.model.Post

class PostsFragmentRecyclerAdapter(private val clickListener: ItemClickedListener) :
    RecyclerView.Adapter<PostsFragmentRecyclerAdapter.PostViewHolder>() {

    var listOfPosts = arrayListOf<Post>()


    interface ItemClickedListener {
        fun onItemClick(post: Post)
    }


    class PostViewHolder (private val itemBinding : ItemPostBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
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

            if (post.image != null) {
                Glide.with(root.context).load(post.image).into(contentImageView)
            }

            if (post.userPhoto != null) {
                Glide.with(root.context).load(post.userPhoto).into(userPhotoImageView)
            } else {
                Glide.with(root.context).load(R.drawable.ic_profile).into(userPhotoImageView)
            }

            root.setOnClickListener {
                clickListener.onItemClick(post)
            }

            likeLinearLayout.setOnClickListener {
                //Toggles the active state of the like button when clicked
                it.isActivated = when (it.isActivated) {
                    true -> false
                    false -> true
                }
                if (it.isActivated) {
                    Toast.makeText(root.context, "like", Toast.LENGTH_SHORT).show()
                }
            }
            commentLinearLayout.setOnClickListener {
                clickListener.onItemClick(post)
            }
            promoteLinearLayout.setOnClickListener {
                Toast.makeText(root.context, "promote", Toast.LENGTH_SHORT).show()
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

    fun setList(list: ArrayList<Post>) {
        this.listOfPosts = list
        notifyDataSetChanged()
    }
}