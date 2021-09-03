package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemCommentBinding
import com.colley.android.model.Comment
import com.colley.android.model.CommentModel


class CommentsFragmentRecyclerAdapter(private val clickListener: ItemClickedListener) :
    RecyclerView.Adapter<CommentsFragmentRecyclerAdapter.CommentViewHolder>() {

    private var listOfComments = arrayListOf<CommentModel>()

    interface ItemClickedListener {
        fun onItemClick(comment: CommentModel)
    }

    class CommentViewHolder (private val itemBinding: ItemCommentBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(comment: CommentModel, clickListener: ItemClickedListener) = with(itemBinding) {

            commentTextTextView.text = comment.commentText
            commenterNameTextView.text = comment.commenterName
            commentTimeStampTextView.text = comment.commentTimeStamp

            if (comment.commenterPhoto != null) {
                Glide.with(root.context).load(comment.commenterPhoto).into(commenterImageView)
            } else {
                Glide.with(root.context).load(R.drawable.ic_profile).into(commenterImageView)
            }

            this.root.setOnClickListener {
                clickListener.onItemClick(comment)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val viewBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = listOfComments[position]
        holder.bind(comment, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfComments.size
    }

    fun setList(list: ArrayList<CommentModel>) {
        this.listOfComments = list
        notifyDataSetChanged()
    }

}