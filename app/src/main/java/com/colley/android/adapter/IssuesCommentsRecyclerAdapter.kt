package com.colley.android.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemCommentBinding
import com.colley.android.databinding.ItemGroupMemberBinding
import com.colley.android.model.Comment
import com.colley.android.model.PrivateChat
import com.colley.android.model.Profile
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class IssuesCommentsRecyclerAdapter(
    private val options: FirebaseRecyclerOptions<Comment>,
    private val currentUser: FirebaseUser?,
    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener,
    private val context: Context,
        ) : FirebaseRecyclerAdapter<Comment, IssuesCommentsRecyclerAdapter.IssueCommentViewHolder>(options) {

    //listener to hide progress bar and display views only when data has been retrieved from database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<Comment>)
    }

    interface ItemClickedListener {
        fun onItemClick(comment: Comment, view: View)
        fun onItemLongCLicked(comment: Comment, view: View)
        fun onUserClicked(userId: String, view: View)
    }

            inner class IssueCommentViewHolder(private val itemBinding: ItemCommentBinding)
                : RecyclerView.ViewHolder(itemBinding.root) {
                fun bind(comment: Comment, clickListener: ItemClickedListener) = with (itemBinding) {

                    //set commenter's name
                    Firebase.database.reference.child("profiles").child(comment.commenterId!!).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val name = snapshot.getValue<Profile>()?.name
                                //check if profile name is not null before setting name
                                if(name != null) {
                                    commenterNameTextView.text = name
                                    //set comment text and time stamp
                                    commentTimeStampTextView.text = comment.commentTimeStamp
                                    commentTextTextView.text = comment.commentText
                                } else {
                                    commenterNameTextView.text = context.getString(R.string.anonymous_text)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }
                    )

                    //load commenter's photo
                    Firebase.database.reference.child("photos").child(comment.commenterId!!).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val photoUrl = snapshot.getValue<String>()
                                //check if photoURl is not null before setting photo
                                if (photoUrl != null) {
                                    Glide.with(context).load(photoUrl).into(commenterImageView)
                                } else {
                                    Glide.with(context).load(R.drawable.ic_profile).into(commenterImageView)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }
                    )

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

    //Callback triggered after all child events in a particular snapshot have been processed.
    //Useful for batch events, such as removing a loading indicator
    override fun onDataChanged() {
        super.onDataChanged()

        //display comments only when data is available,
        //otherwise show progress bar loading
        onDataChangedListener.onDataAvailable(snapshots)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueCommentViewHolder {
        val viewBinding =ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IssueCommentViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: IssueCommentViewHolder, position: Int, model: Comment) {
        holder.bind(model, clickListener)
    }

}