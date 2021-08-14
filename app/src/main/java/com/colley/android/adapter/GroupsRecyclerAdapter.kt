package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemGroupBinding
import com.colley.android.model.ChatGroup
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class GroupsRecyclerAdapter (
    private val options: FirebaseRecyclerOptions<ChatGroup>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener
        )
    : FirebaseRecyclerAdapter<ChatGroup, RecyclerView.ViewHolder>(options) {

    interface ItemClickedListener {
        fun onItemClick(chatGroup: ChatGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val viewBinding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(viewBinding)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: ChatGroup
    ) {
        (holder as GroupViewHolder).bind(model, clickListener)
    }

    class GroupViewHolder (private val itemBinding : ItemGroupBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(chatGroup: ChatGroup, clickListener: ItemClickedListener) = with(itemBinding) {

            groupNameTextView.text = chatGroup.name

            when (chatGroup.unreadMessages) {
                0 -> unreadConversationCountTextView.visibility = View.GONE
                else -> unreadConversationCountTextView.text = chatGroup.unreadMessages.toString()
            }

            when {
                chatGroup.groupIcon !== null ->
                    Glide.with(this.root.context).load(chatGroup.groupIcon).into(groupImageView)
                else ->
                    Glide.with(this.root.context).load(R.drawable.ic_group).into(groupImageView)
            }

            this.root.setOnClickListener {
                clickListener.onItemClick(chatGroup)
            }
        }
    }


}