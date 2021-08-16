package com.colley.android.adapter.group

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.databinding.ItemGroupMemberBinding
import com.colley.android.model.User
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser

class GroupMembersRecyclerAdapter(
        private val options: FirebaseRecyclerOptions<String>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val context: Context
        ) : FirebaseRecyclerAdapter<String, GroupMembersRecyclerAdapter.GroupMemberViewHolder>(options) {

    interface ItemClickedListener {
        fun onItemClick(memberId: String)
    }

            inner class GroupMemberViewHolder(private val itemBinding: ItemGroupMemberBinding)
                : RecyclerView.ViewHolder(itemBinding.root) {
                fun bind(memberId: String, clickListener: ItemClickedListener) = with (itemBinding) {

                    root.setOnClickListener {
                        clickListener.onItemClick(memberId)
                    }

                }
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int, model: String) {
        holder.bind(model, clickListener)
    }
}