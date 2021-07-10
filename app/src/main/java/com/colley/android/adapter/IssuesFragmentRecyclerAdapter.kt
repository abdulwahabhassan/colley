package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.IssuesFragment
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.databinding.ItemIssueBinding
import com.colley.android.databinding.ItemSchoolBinding
import com.colley.android.model.Group
import com.colley.android.model.Issue

class IssuesFragmentRecyclerAdapter : RecyclerView.Adapter<IssuesFragmentRecyclerAdapter.IssueViewHolder>() {

    var listOfIssues = arrayListOf<Issue>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(issue: Issue)
    }

    class IssueViewHolder (private val itemBinding : ItemIssueBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(issue: Issue, clickListener: ItemClickedListener) = with(itemBinding) {


            this.root.setOnClickListener {
                clickListener.onItemClick(issue)
            }
        }
    }

    fun setList(list: ArrayList<Issue>, clickListener: ItemClickedListener) {
        this.listOfIssues = list
        this.clickListener = clickListener
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