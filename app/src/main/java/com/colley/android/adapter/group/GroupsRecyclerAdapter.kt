package com.colley.android.adapter.group

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.model.ChatGroup
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser

class GroupsRecyclerAdapter (
    private val options: FirebaseRecyclerOptions<ChatGroup>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener
        )
    : FirebaseRecyclerAdapter<ChatGroup, GroupsRecyclerAdapter.GroupViewHolder>(options) {

    interface ItemClickedListener {
        fun onItemClick(chatGroup: ChatGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val viewBinding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int, model: ChatGroup) {
        holder.bind(model, clickListener)
    }

    class GroupViewHolder (private val itemBinding : ItemGroupBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(chatGroup: ChatGroup, clickListener: ItemClickedListener) = with(itemBinding) {

            groupNameTextView.text = chatGroup.name

            //hiding unread messages. Feature not implemented yet
            unreadConversationCountTextView.visibility = GONE

            when (chatGroup.groupPhoto) {
                null ->
                    Glide.with(this.root.context).load(R.drawable.ic_group).into(groupImageView)
                else ->
                    Glide.with(this.root.context).load(chatGroup.groupPhoto).into(groupImageView)
            }

            root.setOnClickListener {
                clickListener.onItemClick(chatGroup)
            }
        }
    }


}