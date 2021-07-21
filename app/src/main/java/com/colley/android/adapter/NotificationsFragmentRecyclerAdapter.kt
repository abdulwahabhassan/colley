package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemNotificationBinding
import com.colley.android.model.Notification

class NotificationsFragmentRecyclerAdapter(private val clickListener: ItemClickedListener)
    : RecyclerView.Adapter<NotificationsFragmentRecyclerAdapter.NotificationViewHolder>() {

    var listOfNotifications = arrayListOf<Notification>()

    interface ItemClickedListener {
        fun onItemClick(notification: Notification)
    }

    class NotificationViewHolder(private val itemBinding: ItemNotificationBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(notification: Notification, clickListener: ItemClickedListener) = with(itemBinding) {

            if (notification.image != null) {
                Glide.with(root.context).load(notification.image).into(notificationImageView)
            } else {
                Glide.with(root.context).load(R.drawable.ic_person).into(notificationImageView)
            }
            notificationTextView.text = notification.notificationText
            notificationTimeStampTextView.text = notification.timeStamp

            this.root.setOnClickListener {
                clickListener.onItemClick(notification)
            }
        }
    }

    fun setList(list: ArrayList<Notification>) {
        this.listOfNotifications = list
        notifyDataSetChanged()
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
}