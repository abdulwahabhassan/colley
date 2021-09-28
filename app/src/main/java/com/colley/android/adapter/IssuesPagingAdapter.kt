package com.colley.android.adapter

import android.annotation.SuppressLint
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
import com.colley.android.databinding.ItemIssueBinding
import com.colley.android.model.Issue
import com.colley.android.model.Profile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class IssuesPagingAdapter (
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: IssuePagingItemClickedListener,
        ) : PagingDataAdapter<DataSnapshot, IssueViewHolder>(ISSUE_COMPARATOR) {

    interface IssuePagingItemClickedListener {
        fun onItemClick(issueId: String, view: View)
        fun onItemLongCLicked(issueId: String, view: View)
        fun onUserClicked(userId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val viewBinding = ItemIssueBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return IssueViewHolder(viewBinding)
    }
    override fun onBindViewHolder(viewHolder: IssueViewHolder, position: Int) {
        viewHolder.bind(currentUser, getItem(position), context, clickListener)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }


    companion object {
        private val ISSUE_COMPARATOR = object : DiffUtil.ItemCallback<DataSnapshot>() {
            override fun areItemsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(Issue::class.java)?.issueId == newItem.getValue(Issue::class.java)?.issueId
            }

            override fun areContentsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(Issue::class.java) == newItem.getValue(Issue::class.java)
            }

        }
    }
}

class IssueViewHolder (private val itemBinding : ItemIssueBinding) : RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        currentUser: FirebaseUser?,
        dataSnapshot: DataSnapshot?,
        context: Context,
        clickListener: IssuesPagingAdapter.IssuePagingItemClickedListener) = with(itemBinding) {

        val issue = dataSnapshot?.getValue(Issue::class.java)

        //set issue title, body, timeStamp, contributions and endorsements count
        issueTitleTextView.text = issue?.title
        issueBodyTextView.text = issue?.body
        issueTimeStampTextView.text = issue?.timeStamp

        if(issue?.endorsementsCount != 0) {
            endorsementTextView.visibility = View.VISIBLE
            endorsementTextView.text = issue?.endorsementsCount.toString().removePrefix("-")
        } else {
            endorsementTextView.visibility = View.INVISIBLE
        }

        if(issue?.contributionsCount != 0) {
            contributionsTextView.visibility = View.VISIBLE
            contributionsTextView.text = issue?.contributionsCount.toString()
        } else {
            contributionsTextView.visibility = View.INVISIBLE
        }

        //check if userId is not null
        issue?.userId?.let { userId ->
            //retrieve user profile
            Firebase.database.reference.child("profiles").child(userId).get()
                .addOnSuccessListener { snapShot ->
                    val profile = snapShot.getValue(Profile::class.java)
                    if (profile != null) {
                        //set the name of user who raised this issue
                        userNameTextView.text = profile.name
                        //set the school of the user who raised this issue
                        userSchoolTextView.text = profile.school
                    }
                }

            //retrieve user photo
            Firebase.database.reference.child("photos").child(userId).get()
                .addOnSuccessListener { snapShot ->
                    val photo = snapShot.getValue(String::class.java)
                    //set photo
                    if (photo != null) {
                        Glide.with(root.context).load(photo)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(userImageView)
                    } else {
                        Glide.with(root.context).load(R.drawable.ic_person_light_pearl).into(userImageView)
                    }
                }

        }


        root.setOnClickListener {
            if(issue?.issueId != null) {
                clickListener.onItemClick(issue.issueId, it)
            }
        }

        root.setOnLongClickListener {
            if(issue?.issueId != null) {
                clickListener.onItemLongCLicked(issue.issueId, it)
            }
            true
        }

        userNameTextView.setOnClickListener {
            if(issue?.userId != null) {
                clickListener.onUserClicked(issue.userId, it)
            }
        }
    }

}