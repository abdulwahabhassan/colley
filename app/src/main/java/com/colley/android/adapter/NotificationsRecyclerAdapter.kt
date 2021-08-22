package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.databinding.ItemNotificationBinding
import com.colley.android.model.Notification

class NotificationsRecyclerAdapter : RecyclerView.Adapter<NotificationsRecyclerAdapter.NotificationViewHolder>() {

    var listOfNotifications = arrayListOf<Notification>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(notification: Notification)
    }

    class NotificationViewHolder (private val itemBinding : ItemNotificationBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(notification: Notification, clickListener: ItemClickedListener) = with(itemBinding) {


            this.root.setOnClickListener {
                clickListener.onItemClick(notification)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val viewBinding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = listOfNotifications[position]
        holder.bind(notification, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfNotifications.size
    }

    fun setList(list: ArrayList<Notification>, clickListener: ItemClickedListener) {
        this.listOfNotifications = list
        this.clickListener = clickListener
        notifyDataSetChanged()
    }
}