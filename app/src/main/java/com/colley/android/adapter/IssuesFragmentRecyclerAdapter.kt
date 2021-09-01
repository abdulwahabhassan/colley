package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemIssueBinding
import com.colley.android.model.Issue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class IssuesFragmentRecyclerAdapter (private val clickListener: ItemClickedListener)
    : RecyclerView.Adapter<IssuesFragmentRecyclerAdapter.IssueViewHolder>() {

    private var listOfIssues = arrayListOf<Issue>()

    interface ItemClickedListener {
        fun onItemClick(issue: Issue)
    }

    class IssueViewHolder (private val itemBinding : ItemIssueBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(issue: Issue, clickListener: ItemClickedListener) = with(itemBinding) {



            issueTitleTextView.text = issue.title
            issueBodyTextView.text = issue.body
            issueTimeStampTextView.text = issue.timeStamp
//            userNameTextView.text = issue.userName
//            userSchoolTextView.text = issue.userSchool
            contributionsTextView.text = issue.contributionsCount.toString()
            endorsementTextView.text = issue.endorsementsCount.toString()
//            if (issue.userPhoto != null) {
//                Glide.with(this.root.context).load(issue.userPhoto).into(userImageView)
//            } else {
//                Glide.with(this.root.context).load(R.drawable.ic_profile).into(userImageView)
//            }
            pinImageView.setOnClickListener {
                it.isActivated = when (it.isActivated) {
                    true -> false
                    false -> true
                }
                if (it.isActivated) {
                    Toast.makeText(root.context, "pinned", Toast.LENGTH_SHORT).show()
                }
            }


            this.root.setOnClickListener {
                clickListener.onItemClick(issue)
            }
        }
    }

    fun setList(list: ArrayList<Issue>) {
        this.listOfIssues = list
        //this.clickListener = clickListener
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val viewBinding = ItemIssueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IssueViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        val issue = listOfIssues[position]
        holder.bind(issue, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfIssues.size
    }
}