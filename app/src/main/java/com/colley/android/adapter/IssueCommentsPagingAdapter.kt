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
import com.colley.android.databinding.ItemCommentBinding
import com.colley.android.model.Comment
import com.colley.android.model.Profile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class IssueCommentsPagingAdapter (
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: IssueCommentItemClickedListener,
        ) : PagingDataAdapter<DataSnapshot, IssueCommentViewHolder>(COMMENT_COMPARATOR) {

    interface IssueCommentItemClickedListener {
        fun onItemClick(comment: Comment, view: View)
        fun onItemLongCLicked(comment: Comment, view: View)
        fun onUserClicked(userId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueCommentViewHolder {
        val viewBinding = ItemCommentBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return IssueCommentViewHolder(viewBinding)
    }

    override fun onBindViewHolder(viewHolder: IssueCommentViewHolder, position: Int) {
        viewHolder.bind(getItem(position), context, clickListener)
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
                return oldItem.getValue(Comment::class.java)?.commentId ==
                        newItem.getValue(Comment::class.java)?.commentId
            }

            override fun areContentsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(Comment::class.java) == newItem.getValue(Comment::class.java)
            }

        }
    }
}

class IssueCommentViewHolder(private val itemBinding: ItemCommentBinding)
    : RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(
        dataSnapshot: DataSnapshot?,
        context: Context,
        clickListener: IssueCommentsPagingAdapter.IssueCommentItemClickedListener
    ) =
        with (itemBinding) {

        //parse snapshot to comment model
        val comment = dataSnapshot?.getValue(Comment::class.java)

        //set commenter's name
        Firebase.database.reference.child("profiles").child(comment?.commenterId!!).get()
            .addOnSuccessListener { snapShot ->
                val profile = snapShot.getValue(Profile::class.java)
                //check if profile name is not null before setting name
                if(profile != null) {
                    commenterNameTextView.text = profile.name
                    //set comment text and time stamp
                    commentTimeStampTextView.text = comment.commentTimeStamp
                    commentTextTextView.text = comment.commentText
                } else {
                    commenterNameTextView.text = context.getString(R.string.anonymous_text)
                }
        }

        //load commenter's photo
        Firebase.database.reference.child("photos").child(comment.commenterId).get()
            .addOnSuccessListener { snapShot ->
                val photoUrl = snapShot.getValue(String::class.java)
                //load photoUrl to view
                if (photoUrl != null) {
                    Glide.with(context).load(photoUrl).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(commenterImageView)
                } else {
                    Glide.with(context).load(R.drawable.ic_person_light_pearl)
                        .into(commenterImageView)
                }
            }

        //on long click
        root.setOnLongClickListener {
            clickListener.onItemLongCLicked(comment, it)
            true
        }

        //on click
        root.setOnClickListener {
            clickListener.onItemClick(comment, it)
        }

        commenterImageView.setOnClickListener {
            clickListener.onUserClicked(comment.commenterId, it)
        }

    }
}