package com.colley.android.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.model.PostModel

class PostsFragmentRecyclerAdapter(private val clickListener: ItemClickedListener) :
    RecyclerView.Adapter<PostsFragmentRecyclerAdapter.PostViewHolder>() {

    var listOfPosts = arrayListOf<PostModel>()


    interface ItemClickedListener {
        fun onItemClick(postModel: PostModel)
    }


    class PostViewHolder (private val itemBinding : ItemPostBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(postModel: PostModel, clickListener: ItemClickedListener) = with(itemBinding) {

            nameTextView.text = postModel.name
            schoolTextView.text = postModel.school
            timeStampTextView.text = postModel.timeStamp
            contentTextView.text = postModel.text

            if(postModel.location == null) {
                locationTextView.visibility = GONE
            } else {
                locationTextView.text = postModel.location
            }

            when (postModel.likes) {
                0 -> likeCountTextView.visibility = GONE
                1 -> likeCountTextView.text = "${postModel.likes} like"
                else -> likeCountTextView.text = "${postModel.likes} likes"
            }

            when (postModel.comments) {
                0 -> commentCountTextView.visibility = GONE
                1 -> commentCountTextView.text = "${postModel.comments} comment"
                else -> commentCountTextView.text = "${postModel.comments} comments"
            }

            when (postModel.promotions) {
                0 -> promotionCountTextView.visibility = GONE
                1 -> promotionCountTextView.text = "${postModel.promotions} promotion"
                else -> promotionCountTextView.text = "${postModel.promotions} promotions"
            }

                Glide.with(root.context).load(postModel.image).into(contentImageView)

            if (postModel.userPhoto != null) {
                Glide.with(root.context).load(postModel.userPhoto).into(userPhotoImageView)
            } else {
                Glide.with(root.context).load(R.drawable.ic_profile).into(userPhotoImageView)
            }

            contentTextView.setOnClickListener {
                clickListener.onItemClick(postModel)
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
                clickListener.onItemClick(postModel)
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

    fun setList(list: ArrayList<PostModel>) {
        this.listOfPosts = list
        notifyDataSetChanged()
    }
}