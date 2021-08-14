package com.colley.android.adapter.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.databinding.ItemGroupMemberBinding
import com.colley.android.templateModel.GroupMember

class AddGroupMembersRecyclerAdapter(
    private val clickListener: ItemClickedListener
) :
    RecyclerView.Adapter<AddGroupMembersRecyclerAdapter.GroupMemberViewHolder>() {

    private var listOfGroupMember = arrayListOf<GroupMember>()

    interface ItemClickedListener {
        fun onItemClick(groupMember: GroupMember)
    }

    class GroupMemberViewHolder (private val itemBinding : ItemGroupMemberBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(groupMember: GroupMember, clickListener: ItemClickedListener) = with(itemBinding) {

            addGroupMemberCheckBox.setOnClickListener {}

            //set name
            groupMemberNameTextView.text = groupMember.name
            //load photo
            Glide.with(root.context).load(groupMember.userPhoto).into(groupMemberImageView)

            this.root.setOnClickListener {
                clickListener.onItemClick(groupMember)
            }
        }
    }

    fun setList(list: ArrayList<GroupMember>) {
        this.listOfGroupMember = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int) {
        val groupMember = listOfGroupMember[position]
        holder.bind(groupMember, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfGroupMember.size
    }
}