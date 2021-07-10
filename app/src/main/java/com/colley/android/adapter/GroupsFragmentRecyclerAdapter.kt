package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.model.Group
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.databinding.ItemSchoolBinding
import com.colley.android.model.School

class GroupsFragmentRecyclerAdapter : RecyclerView.Adapter<GroupsFragmentRecyclerAdapter.GroupViewHolder>() {

    var listOfGroups = arrayListOf<Group>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(group: Group)
    }

    class GroupViewHolder (private val itemBinding : ItemGroupBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(group: Group, clickListener: ItemClickedListener) = with(itemBinding) {


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