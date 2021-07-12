package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.model.Group
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.databinding.ItemSchoolBinding
import com.colley.android.model.School
import com.google.android.material.internal.TextDrawableHelper

class GroupsFragmentRecyclerAdapter : RecyclerView.Adapter<GroupsFragmentRecyclerAdapter.GroupViewHolder>() {

    private var listOfGroups = arrayListOf<Group>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(group: Group)
    }

    class GroupViewHolder (private val itemBinding : ItemGroupBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(group: Group, clickListener: ItemClickedListener) = with(itemBinding) {

            groupNameTextView.text = group.name

            when (group.unreadMessages) {
                0 -> unreadConversationCountTextView.visibility = GONE
                else -> unreadConversationCountTextView.text = group.unreadMessages.toString()
            }

            when {
                group.groupIcon !== null ->
                    Glide.with(this.root.context).load(group.groupIcon).into(groupIconImageView)
                else ->
                    Glide.with(this.root.context).load(R.drawable.ic_group).into(groupIconImageView)
            }

            this.root.setOnClickListener {
                clickListener.onItemClick(group)
            }
        }
    }

    fun setList(list: ArrayList<Group>, clickListener: ItemClickedListener) {
        this.listOfGroups = list
        this.clickListener = clickListener
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val viewBinding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = listOfGroups[position]
        holder.bind(group, clickListener)
    }

    override fun getItemCount(): Int {
       return listOfGroups.size
    }
}