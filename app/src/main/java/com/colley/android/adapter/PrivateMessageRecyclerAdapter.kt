package com.colley.android.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.*
import com.colley.android.model.GroupMessage
import com.colley.android.model.PrivateChat
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PrivateMessageRecyclerAdapter(
    private val options: FirebaseRecyclerOptions<PrivateChat>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val onDataChangedListener: DataChangedListener,
    private val context: Context
) : FirebaseRecyclerAdapter<PrivateChat, RecyclerView.ViewHolder>(options) {

    //listener to hide progress bar and display views only when data has been retrieved from database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateChat>)
    }

    interface ItemClickedListener {
        fun onItemLongCLicked(message: GroupMessage, view: View)
        fun onUserClicked(userId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        //return viewholder depending on viewType specified
        var viewHolder : RecyclerView.ViewHolder? = null

        when (viewType) {
            VIEW_TYPE_CURRENT_USER -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_current_user,
                    parent,
                    false)
                val binding = ItemPrivateMessageCurrentUserBinding.bind(view)
                viewHolder = CurrentUserPrivateMessageViewHolder(binding)
            }
            VIEW_TYPE_OTHER_USER -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_other_user,
                    parent,
                    false)
                val binding = ItemPrivateMessageOtherUserBinding.bind(view)
                viewHolder = OtherUserPrivateMessageViewHolder(binding)
            }
            VIEW_TYPE_CURRENT_USER_SAME -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_current_user_same,
                    parent,
                    false)
                val binding = ItemPrivateMessageCurrentUserSameBinding.bind(view)
                viewHolder = CurrentUserPrivateMessageViewHolderSame(binding)
            }
            VIEW_TYPE_OTHER_USER_SAME -> {
                val view = inflater.inflate(
                    R.layout.item_private_message_other_user_same,
                    parent,
                    false)
                val binding = ItemPrivateMessageOtherUserSameBinding.bind(view)
                viewHolder = OtherUserPrivateMessageViewHolderSame(binding)
            }
        }
        return viewHolder!!
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: PrivateChat
    ) {

        val uid = currentUser?.uid

        if (snapshots[position].fromUserId != uid) {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].fromUserId == snapshots[position - 1].fromUserId) {
                    (holder as OtherUserPrivateMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as OtherUserPrivateMessageViewHolder).bind(model, position)
                }
            }

        } else {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].fromUserId == snapshots[position - 1].fromUserId) {
                    (holder as CurrentUserPrivateMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as CurrentUserPrivateMessageViewHolder).bind(model, position)
                }
            }
        }

    }

    //Callback triggered after all child events in a particular snapshot have been processed.
    //Useful for batch events, such as removing a loading indicator
    override fun onDataChanged() {
        super.onDataChanged()
        //display GroupMessageFragment EditText layout only when data has been bound,
        //otherwise show progress bar loading
        onDataChangedListener.onDataAvailable(snapshots)
    }


    override fun getItemViewType(position: Int): Int {

        //deduce and return the appropriate view type for view holder
        var viewType = 0

        val uid = currentUser?.uid
        if (snapshots[position].fromUserId != uid) {
            if (snapshots.size != 0) {
                viewType = if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                   VIEW_TYPE_OTHER_USER_SAME
                } else {
                    VIEW_TYPE_OTHER_USER
                }
            }
        } else {
            if (snapshots.size != 0) {
                viewType = if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                    VIEW_TYPE_CURRENT_USER_SAME
                } else {
                    VIEW_TYPE_CURRENT_USER
                }
            }
        }
        return viewType
    }

    inner class CurrentUserPrivateMessageViewHolder(
        private val binding: ItemPrivateMessageCurrentUserBinding
    )
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PrivateChat, itemPosition: Int) {

            //set message body
            if (item.text != null) {
                binding.currentUserMessageTextView.text = item.text
                binding.currentUserMessageTextView.visibility = VISIBLE
            } else {
                binding.currentUserMessageTextView.visibility = GONE
            }


            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.currentUserMessagePhotoImageView, item.image!!)
                binding.currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                binding.currentUserMessagePhotoImageView.visibility = GONE
            }


        }

    }

    inner class CurrentUserPrivateMessageViewHolderSame(
        private val binding: ItemPrivateMessageCurrentUserSameBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PrivateChat, itemPosition: Int) {

            //set message body
            if (item.text != null) {
                binding.currentUserMessageTextView.text = item.text
                binding.currentUserMessageTextView.visibility = VISIBLE
            } else {
                binding.currentUserMessageTextView.visibility = GONE
            }

            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.currentUserMessagePhotoImageView, item.image!!)
                binding.currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                binding.currentUserMessagePhotoImageView.visibility = GONE
            }

        }

    }

    inner class OtherUserPrivateMessageViewHolder(
        private val binding: ItemPrivateMessageOtherUserBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PrivateChat, itemPosition: Int) {

            //set message body
            if (item.text != null) {
                binding.messageTextView.text = item.text
                binding.messageTextView.visibility = VISIBLE
            } else {
                binding.messageTextView.visibility = GONE
            }


            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.messagePhotoImageView, item.image!!)
                binding.messagePhotoImageView.visibility = VISIBLE
            } else {
                binding.messagePhotoImageView.visibility = GONE
            }
        }

    }

    inner class OtherUserPrivateMessageViewHolderSame(
        private val binding: ItemPrivateMessageOtherUserSameBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PrivateChat, itemPosition: Int) {


            //set message body
            if (item.text != null) {
                binding.messageTextView.text = item.text
                binding.messageTextView.visibility = VISIBLE
            } else {
                binding.messageTextView.visibility = GONE
            }


            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.messagePhotoImageView, item.image!!)
                binding.messagePhotoImageView.visibility = VISIBLE
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
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(imageView)
                    imageView.visibility = VISIBLE
                }
                .addOnFailureListener { e ->
                    Log.w(
                        GroupMessageRecyclerAdapter.TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(imageView.context).load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(imageView)
            imageView.visibility = VISIBLE
        }
    }

    companion object {
        const val TAG = "MessageAdapter"

        //four view types
        var VIEW_TYPE_CURRENT_USER = 0
        var VIEW_TYPE_OTHER_USER = 1
        var VIEW_TYPE_CURRENT_USER_SAME = 2
        var VIEW_TYPE_OTHER_USER_SAME = 3
    }

}

