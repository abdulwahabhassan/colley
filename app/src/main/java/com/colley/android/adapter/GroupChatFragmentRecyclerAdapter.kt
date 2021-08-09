package com.colley.android.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemGroupMessageBinding
import com.colley.android.databinding.ItemGroupMessageCurrentUserBinding
import com.colley.android.templateModel.GroupMessage
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class GroupChatFragmentRecyclerAdapter(
    private val options: FirebaseRecyclerOptions<GroupMessage>,
    private val currentUser: FirebaseUser?,
    private val onBindViewHolderListener: BindViewHolderListener
) : FirebaseRecyclerAdapter<GroupMessage, RecyclerView.ViewHolder>(options) {

    interface BindViewHolderListener {
        fun onBind()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_GROUP_MEMBER) {
            val view = inflater.inflate(R.layout.item_group_message, parent, false)
            val binding = ItemGroupMessageBinding.bind(view)
            GroupMessageViewHolder(binding)
        } else {
            val view = inflater.inflate(R.layout.item_group_message_current_user, parent, false)
            val binding = ItemGroupMessageCurrentUserBinding.bind(view)
            CurrentUserMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: GroupMessage
    ) {

        if (options.snapshots[position].name != currentUser?.displayName) {
            (holder as GroupMessageViewHolder).bind(model)
        } else {
            (holder as CurrentUserMessageViewHolder).bind(model)
        }
        onBindViewHolderListener.onBind()

    }

    override fun getItemViewType(position: Int): Int {
        return if (options.snapshots[position].name != currentUser?.displayName) VIEW_TYPE_GROUP_MEMBER
        else VIEW_TYPE_CURRENT_USER
    }

    inner class CurrentUserMessageViewHolder(private val binding: ItemGroupMessageCurrentUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {

            //load user photo
            if (currentUser != null) {
                loadImageIntoView(binding.currentUserImageView,
                    currentUser.photoUrl.toString()
                )
            }

            //set message body
            if (item.text != null) {
                // "I am fine and I am making progress, so one love my bro. This is what it is now"
                binding.currentUserMessageTextView.text = item.text
            } else {
                binding.currentUserMessageTextView.visibility = GONE
            }

            //set username
            binding.currentUserNameTextView.text = currentUser?.displayName

            //load message photo if any
            if (item.photoUrl != null) {
                loadImageIntoView(binding.currentUserMessagePhotoImageView, item.photoUrl!!)
            } else {
                binding.currentUserMessagePhotoImageView.visibility = GONE
            }
        }

    }


    inner class GroupMessageViewHolder(private val binding: ItemGroupMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage) {
            //load user photo
            if (item.userPhoto != null) {
                loadImageIntoView(binding.messengerImageView, item.userPhoto!!)
            }
            //set message body
            if (item.text != null) {
               // "I am fine and I am making progress, so one love my bro. This is what it is now"
                binding.messageTextView.text = item.text
            } else {
                binding.messageTextView.visibility = GONE
            }

            //set username
            binding.messengerNameTextView.text = if (item.name == null) "Anonymous" else item.name
            //load message photo if any
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messagePhotoImageView, item.photoUrl!!)
            } else {
                binding.messagePhotoImageView.visibility = GONE
            }
        }

    }

    //image loader
    private fun loadImageIntoView(imageView: ShapeableImageView, photoUrl: String) {
        if (photoUrl.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(photoUrl)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Glide.with(imageView.context)
                        .load(downloadUrl)
                        .into(imageView)
                }
                .addOnFailureListener { e ->
                    Log.w(
                        TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(imageView.context).load(photoUrl).into(imageView)
        }
    }

    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_CURRENT_USER = 1
        const val VIEW_TYPE_GROUP_MEMBER = 2
    }

}

