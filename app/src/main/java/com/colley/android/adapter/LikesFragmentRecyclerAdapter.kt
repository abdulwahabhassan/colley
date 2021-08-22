package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemLikeBinding
import com.colley.android.model.Like

class LikesFragmentRecyclerAdapter(private val clickListener: ItemClickedListener)
    : RecyclerView.Adapter<LikesFragmentRecyclerAdapter.LikeViewHolder>() {


    var listOfLikes = arrayListOf<Like>()

    interface ItemClickedListener {
        fun onItemClick(like: Like)
    }

    class LikeViewHolder(private val itemBinding : ItemLikeBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(like: Like, clickListener: ItemClickedListener) = with(itemBinding) {

            userNameTextView.text = like.userName
            if (like.userPhoto != null) {
                Glide.with(root.context).load(like.userPhoto).into(userImageView)
            } else {
                Glide.with(root.context).load(R.drawable.ic_profile).into(userImageView)
            }

            Glide.with(root.context).load(R.drawable.ic_like_active).into(likeLogoImageView)

            this.root.setOnClickListener {
                clickListener.onItemClick(like)
            }
        }
    }

    fun setList(list: ArrayList<Like>) {
        this.listOfLikes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
        val viewBinding = ItemLikeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LikeViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: LikeViewHolder, position: Int) {
        val like = listOfLikes[position]
        holder.bind(like, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfLikes.size
    }
}