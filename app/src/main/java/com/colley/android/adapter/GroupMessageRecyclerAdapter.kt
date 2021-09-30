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
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class GroupMessageRecyclerAdapter(
    options: FirebaseRecyclerOptions<GroupMessage>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val onDataChangedListener: DataChangedListener,
    private val context: Context
) : FirebaseRecyclerAdapter<GroupMessage, RecyclerView.ViewHolder>(options) {

    //listener to hide progress bar and display views only when data has been retrieved from
    //database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable()
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
                    R.layout.item_group_message_current_user,
                    parent,
                    false)
                val binding = ItemGroupMessageCurrentUserBinding.bind(view)
                viewHolder = CurrentUserMessageViewHolder(binding)
            }
            VIEW_TYPE_GROUP_MEMBER -> {
                val view = inflater.inflate(
                    R.layout.item_group_message_other_user,
                    parent,
                    false)
                val binding = ItemGroupMessageOtherUserBinding.bind(view)
                viewHolder = GroupMessageViewHolder(binding)
            }
            VIEW_TYPE_CURRENT_USER_SAME -> {
                val view = inflater.inflate(
                    R.layout.item_group_message_current_user_same,
                    parent,
                    false)
                val binding = ItemGroupMessageCurrentUserSameBinding.bind(view)
                viewHolder = CurrentUserMessageViewHolderSame(binding)
            }
            VIEW_TYPE_GROUP_MEMBER_SAME -> {
                val view = inflater.inflate(
                    R.layout.item_group_message_other_user_same,
                    parent,
                    false)
                val binding = ItemGroupMessageOtherUserSameBinding.bind(view)
                viewHolder = GroupMessageViewHolderSame(binding)
            }
        }
        return viewHolder!!
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        //clear views (make invisible) when they are no longer in view and about to be used for recycling
        if (holder.itemViewType == VIEW_TYPE_GROUP_MEMBER) {
            (holder as GroupMessageViewHolder).clear()
        }

    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: GroupMessage
    ) {
        val uid = currentUser?.uid

        if (snapshots[position].userId != uid) {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].userId == snapshots[position - 1].userId) {
                    (holder as GroupMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as GroupMessageViewHolder).bind(model, position)
                }
            }

        } else {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].userId == snapshots[position - 1].userId) {
                    (holder as CurrentUserMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as CurrentUserMessageViewHolder).bind(model, position)
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
        onDataChangedListener.onDataAvailable()
    }

    override fun getItemViewType(position: Int): Int {

        //deduce and return the appropriate view type for view holder
        var viewType = 0

        val uid = currentUser?.uid
        if (snapshots[position].userId != uid) {
            if (snapshots.size != 0) {
                viewType = if(position > 0 && snapshots[position].userId == snapshots[position - 1].userId) {
                    VIEW_TYPE_GROUP_MEMBER_SAME
                } else {
                    VIEW_TYPE_GROUP_MEMBER
                }
            }
        } else {
            if (snapshots.size != 0) {
                viewType = if(position > 0 && snapshots[position].userId == snapshots[position - 1].userId) {
                    VIEW_TYPE_CURRENT_USER_SAME
                } else {
                    VIEW_TYPE_CURRENT_USER
                }
            }
        }
        return viewType
    }


    inner class CurrentUserMessageViewHolder(
        private val binding: ItemGroupMessageCurrentUserBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, itemPosition: Int) {

            //load user photo
//            Firebase.database.reference.child("photos").child(item.userId!!).get()
//                .addOnSuccessListener { dataSnapshot ->
//                    val photo = dataSnapshot.getValue<String?>()
//                    if (snapshots.size != 0) {
//                        //if the next message is from the same user, remove userName from the next
//                            //message
//                        if(itemPosition > 0 && snapshots[itemPosition].userId ==
//                            snapshots[itemPosition - 1].userId) {
//                            binding.currentUserImageView.visibility = INVISIBLE
//                        } else {
//                            if (photo != null) {
//                                loadImageIntoView(binding.currentUserImageView, photo)
//                            } else {
//                                Glide.with(context).load(R.drawable.ic_person_light_apricot)
//                                    .into(binding.currentUserImageView)
//                                binding.currentUserImageView.visibility = VISIBLE
//                            }
//                        }
//                    }
//                }

            //set message body
            if (item.text != null) {
                binding.currentUserMessageTextView.text = item.text
                binding.currentUserMessageTextView.visibility = VISIBLE
            } else {
                binding.currentUserMessageTextView.visibility = GONE
            }

//            //set username
//            Firebase.database.reference.child("profiles").child(item.userId!!)
//                .child("name").get().addOnSuccessListener { dataSnapshot ->
//                    val name = dataSnapshot.getValue<String>()
//                    if (snapshots.size != 0) {
//                        //if the next message is from the same user as in the previous message,
//                            //remove userPhoto from the next message
//                        if(itemPosition > 0 && snapshots[itemPosition].userId ==
//                            snapshots[itemPosition - 1].userId) {
//                            binding.currentUserNameTextView.visibility = GONE
//                        } else {
//                            binding.currentUserNameTextView.text = name
//                            binding.currentUserNameTextView.visibility = VISIBLE
//                        }
//                    }
//                }


            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.currentUserMessagePhotoImageView, item.image!!)
                binding.currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                binding.currentUserMessagePhotoImageView.visibility = GONE
            }


        }

    }

    inner class CurrentUserMessageViewHolderSame(
        private val binding: ItemGroupMessageCurrentUserSameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, itemPosition: Int) {

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

    inner class GroupMessageViewHolder(private val binding: ItemGroupMessageOtherUserBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, itemPosition: Int) {

            //view user profile when image is clicked
            binding.messengerImageView.setOnClickListener {
                item.userId?.let { it1 -> clickListener.onUserClicked(it1, it) }
            }

            //load user photo
            Firebase.database.reference.child("photos").child(item.userId!!).get()
                .addOnSuccessListener {  dataSnapshot ->
                    val photo = dataSnapshot.getValue(String::class.java)
                    if (snapshots.size != 0) {
                        if(itemPosition > 0 && snapshots[itemPosition].userId ==
                            snapshots[itemPosition - 1].userId) {
                            binding.messengerImageView.visibility = GONE
                        } else {
                            if (photo != null) {
                                loadImageIntoView(binding.messengerImageView, photo)
                            } else {
                                Glide.with(context).load(R.drawable.ic_person_light_pearl)
                                    .into(binding.messengerImageView)
                                binding.messengerImageView.visibility = VISIBLE
                            }
                        }
                    }
            }

            //set message body
            if (item.text != null) {
                binding.messageTextView.text = item.text
                binding.messageTextView.visibility = VISIBLE
            } else {
                binding.messageTextView.visibility = GONE
            }

            //set username
            Firebase.database.reference.child("profiles").child(item.userId!!)
                .child("name").get()
                .addOnSuccessListener { dataSnapshot ->
                    val name = dataSnapshot.getValue<String>()
                    if (snapshots.size != 0) {
                        if(itemPosition > 0 && snapshots[itemPosition].userId ==
                            snapshots[itemPosition - 1].userId) {
                            binding.messengerNameTextView.visibility = GONE
                        } else {
                            binding.messengerNameTextView.text = name
                            binding.messengerNameTextView.visibility = VISIBLE
                        }
                    }
                }


            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.messagePhotoImageView, item.image!!)
                binding.messagePhotoImageView.visibility = VISIBLE
            } else {
                binding.messagePhotoImageView.visibility = GONE
            }
        }

        fun clear() {
            binding.messengerNameTextView.visibility = INVISIBLE
            binding.messengerImageView.visibility = INVISIBLE
        }

    }

    inner class GroupMessageViewHolderSame(
        private val binding: ItemGroupMessageOtherUserSameBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, itemPosition: Int) {


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
                        TAG,
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
        var VIEW_TYPE_GROUP_MEMBER = 1
        var VIEW_TYPE_CURRENT_USER_SAME = 2
        var VIEW_TYPE_GROUP_MEMBER_SAME = 3
    }


}

