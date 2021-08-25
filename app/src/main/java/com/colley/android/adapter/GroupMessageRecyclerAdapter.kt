package com.colley.android.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View.*
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemGroupMessageBinding
import com.colley.android.databinding.ItemGroupMessageCurrentUserBinding
import com.colley.android.model.Profile
import com.colley.android.model.GroupMessage
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class GroupMessageRecyclerAdapter(
    private val options: FirebaseRecyclerOptions<GroupMessage>,
    private val currentUser: FirebaseUser?,
    private val onBindViewHolderListener: BindViewHolderListener,
    private val context: Context
) : FirebaseRecyclerAdapter<GroupMessage, RecyclerView.ViewHolder>(options) {

    //listener to hide progress bar and display views only when data has been retrieved from database and bound to view holder
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
        val uid = currentUser?.uid
        if (options.snapshots[position].userId != uid) {

            (holder as GroupMessageViewHolder).bind(model, position)
        } else {
            (holder as CurrentUserMessageViewHolder).bind(model, position)
        }

        //display GroupChatFragment EditText layout only when data has been bound,
        //otherwise show progress bar loading
        onBindViewHolderListener.onBind()

    }


    override fun getItemViewType(position: Int): Int {
        val uid = currentUser?.uid
        return if (options.snapshots[position].userId != uid) VIEW_TYPE_GROUP_MEMBER
        else VIEW_TYPE_CURRENT_USER
    }

    inner class CurrentUserMessageViewHolder(private val binding: ItemGroupMessageCurrentUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, itemPosition: Int) {

            //load user photo
            Firebase.database.reference.child("photos").child(item.userId!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String?>()
                        //if the next message is from the same user, remove userName from the next message
                        if(itemPosition > 0 && options.snapshots[itemPosition].userId == options.snapshots[itemPosition - 1].userId) {
                            binding.currentUserImageView.visibility = GONE
                        } else {
                            if (photo != null) {
                                loadImageIntoView(binding.currentUserImageView, photo)
                            } else {
                                Glide.with(context).load(R.drawable.ic_person).into(binding.currentUserImageView)
                                binding.currentUserImageView.visibility = VISIBLE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getProfilePhoto:onCancelled", error.toException())
                    }
                }
            )

            //set message body
            if (item.text != null) {
                binding.currentUserMessageTextView.text = item.text
                binding.currentUserMessageTextView.visibility = VISIBLE
            } else {
                binding.currentUserMessageTextView.visibility = GONE
            }

            //set username
            Firebase.database.reference.child("profiles").child(item.userId!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val profile = snapshot.getValue<Profile>()
                        //if the next message is from the same user as in the previous message, remove userPhoto from the next message
                        if(itemPosition > 0 && options.snapshots[itemPosition].userId == options.snapshots[itemPosition - 1].userId) {
                            binding.currentUserNameTextView.visibility = GONE
                        } else {
                            binding.currentUserNameTextView.text = profile?.name
                            binding.currentUserNameTextView.visibility = VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getProfileName:onCancelled", error.toException())
                    }
                }
            )

            //load message photo if any
            if (item.image != null) {
                loadImageIntoView(binding.currentUserMessagePhotoImageView, item.image!!)
                binding.currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                binding.currentUserMessagePhotoImageView.visibility = GONE
            }
        }

    }


    inner class GroupMessageViewHolder(private val binding: ItemGroupMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupMessage, itemPosition: Int) {

            //load user photo
            Firebase.database.reference.child("photos").child(item.userId!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String?>()
                        if(itemPosition > 0 && options.snapshots[itemPosition].userId == options.snapshots[itemPosition - 1].userId) {
                            binding.messengerImageView.visibility = GONE
                        } else {
                            if (photo != null) {
                                loadImageIntoView(binding.messengerImageView, photo)
                            } else {
                                Glide.with(context).load(R.drawable.ic_person).into(binding.messengerImageView)
                                binding.messengerImageView.visibility = VISIBLE
                            }
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getProfilePhoto:onCancelled", error.toException())
                    }
                }
            )


            //set message body
            if (item.text != null) {
                binding.messageTextView.text = item.text
                binding.messageTextView.visibility = VISIBLE
            } else {
                binding.messageTextView.visibility = GONE
            }

            //set username
            Firebase.database.reference.child("profiles").child(item.userId!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val profile = snapshot.getValue<Profile>()
                        if(itemPosition > 0 && options.snapshots[itemPosition].userId == options.snapshots[itemPosition - 1].userId) {
                            binding.messengerNameTextView.visibility = GONE
                        } else {
                            binding.messengerNameTextView.text = profile?.name
                            binding.messengerNameTextView.visibility = VISIBLE
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getProfileName:onCancelled", error.toException())
                    }
                }
            )

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
            Glide.with(imageView.context).load(photoUrl).into(imageView)
            imageView.visibility = VISIBLE
        }
    }


    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_CURRENT_USER = 1
        const val VIEW_TYPE_GROUP_MEMBER = 2
    }


}

