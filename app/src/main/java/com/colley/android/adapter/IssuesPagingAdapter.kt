package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.ItemIssueBinding
import com.colley.android.model.Issue
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

class IssuesPagingAdapter(
    options: DatabasePagingOptions<Issue>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: IssuePagingItemClickedListener,
) : FirebaseRecyclerPagingAdapter<Issue, IssuePagingViewHolder>(options) {

    interface IssuePagingItemClickedListener {
        fun onItemClick(issueId: String, view: View)
        fun onItemLongCLicked(issueId: String, view: View)
        fun onUserClicked(userId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssuePagingViewHolder {
        val viewBinding = ItemIssueBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return IssuePagingViewHolder(viewBinding)
    }

    override fun onBindViewHolder(viewHolder: IssuePagingViewHolder, position: Int, model: Issue) {
        viewHolder.bind(currentUser, model, context, clickListener)
    }

    //returns a unique view for each item
    override fun getItemViewType(position: Int): Int {
        return position
    }

}


class IssuePagingViewHolder (private val itemBinding : ItemIssueBinding) : RecyclerView.ViewHolder(itemBinding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        currentUser: FirebaseUser?,
        issue: Issue,
        context: Context,
        clickListener: IssuesPagingAdapter.IssuePagingItemClickedListener) = with(itemBinding) {

        //set issue title, body, timeStamp, contributions and endorsements count
        issueTitleTextView.text = issue.title
        issueBodyTextView.text = issue.body
        issueTimeStampTextView.text = issue.timeStamp

        if(issue.endorsementsCount != 0) {
            endorsementTextView.visibility = VISIBLE
            endorsementTextView.text = issue.endorsementsCount.toString()
        } else {
            endorsementTextView.visibility = INVISIBLE
        }

        if(issue.contributionsCount != 0) {
            contributionsTextView.visibility = VISIBLE
            contributionsTextView.text = issue.contributionsCount.toString()
        } else {
            contributionsTextView.visibility = INVISIBLE
        }

        //check if userId is not null
        issue.userId?.let { userId ->
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
                    Glide.with(root.context).load(R.drawable.ic_profile).into(userImageView)
                }
            }

        }


        root.setOnClickListener {
            if(issue.issueId != null) {
                clickListener.onItemClick(issue.issueId, it)
            }
        }

        root.setOnLongClickListener {
            if(issue.issueId != null) {
                clickListener.onItemLongCLicked(issue.issueId, it)
            }
            true
        }

        userNameTextView.setOnClickListener {
            if(issue.userId != null) {
                clickListener.onUserClicked(issue.userId, it)
            }
        }
    }

}