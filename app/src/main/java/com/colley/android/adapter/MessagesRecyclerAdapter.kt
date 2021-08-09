package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.databinding.ItemMessageBinding
import com.colley.android.templateModel.Message

class MessagesRecyclerAdapter : RecyclerView.Adapter<MessagesRecyclerAdapter.MessageViewHolder>() {

    var listOfMessages = arrayListOf<Message>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(message: Message)
    }

    class MessageViewHolder (private val itemBinding : ItemMessageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: Message, clickListener: ItemClickedListener) = with(itemBinding) {


            this.root.setOnClickListener {
                clickListener.onItemClick(message)
            }
        }
    }

    fun setList(list: ArrayList<Message>, clickListener: ItemClickedListener) {
        this.listOfMessages = list
        this.clickListener = clickListener
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val viewBinding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = listOfMessages[position]
        holder.bind(message, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfMessages.size
    }
}