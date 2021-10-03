package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.colley.android.R
import com.colley.android.databinding.*
import com.colley.android.glide.GlideImageLoader
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

    //list to keep tracked of selected messages
    private var messagesSelectedList = arrayListOf<String>()
    //list to keep tracked of selected views
    private var viewsSelectedList = arrayListOf<View>()

    //function to clear selected messages tracking list when action mode is off
    fun resetMessagesSelectedList() {
        messagesSelectedList.clear()
    }
    //rest back resources for all selected views when action mode is destroyed
    fun restBackgroundOfSelectedViews() {
        viewsSelectedList.forEach { view ->
            view.setBackgroundResource(R.color.white)
        }
        viewsSelectedList.clear()
    }

    //listener to hide progress bar and display views only when data has been retrieved from
    //database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable()
    }

    interface ItemClickedListener {
        fun onItemLongCLicked(message: GroupMessage, view: View)
        fun onUserClicked(userId: String, view: View)
        fun onItemClicked(message: GroupMessage, root: View)
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
        private val binding: ItemGroupMessageCurrentUserBinding
        )
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage, itemPosition: Int) = with(binding) {

            //set message body
            if (message.text != null) {
                currentUserMessageTextView.text = message.text
                currentUserMessageTextView.visibility = VISIBLE
            } else {
                currentUserMessageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadMessageImageIntoView(currentUserMessagePhotoImageView, message.image!!, photoProgressBar)
                currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                currentUserMessagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener { root ->
                //only work if action mode is on, we know this when messagesSelectedList is not empty
                //as a result of OnLongClick being already triggered or resetMessageSelectedList
                //hasn't been called
                if(messagesSelectedList.isNotEmpty()) {
                    //a list of selected messages is used to keep track of selected messages to avoid
                    //inconsistency when views are recycled
                    if (messagesSelectedList.contains(message.messageId)) {
                        //remove message id if they have already been selected
                        messagesSelectedList.remove(message.messageId)
                        //set background color to indicate deselection
                        root.setBackgroundResource(R.color.white)
                    } else {
                        //add message id if they haven't be selected
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        //set background color to indicate selection
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    //update list of views
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, root)
                }
            }

            root.setOnLongClickListener {
                //a list of selected messages is used to keep track of selected messages to avoid
                //inconsistency when views are recycled
                if (messagesSelectedList.contains(message.messageId)) {
                    //remove message if they have already been selected
                    messagesSelectedList.remove(message.messageId)
                    //set background color to indicate deselection
                    root.setBackgroundResource(R.color.white)
                } else {
                    //add message if they haven't be selected
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    //set background color to indicate selection
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                //update list of views
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }

            //during onBindViewHolder, which may occur when views are recycled, we use the tracking
            //list of selected messages to keep the background resource of each message's view set
            //to the appropriate color
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }
        }

    }

    inner class CurrentUserMessageViewHolderSame(
        private val binding: ItemGroupMessageCurrentUserSameBinding
        ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage, itemPosition: Int) = with(binding) {

            //set message body
            if (message.text != null) {
                currentUserMessageTextView.text = message.text
                currentUserMessageTextView.visibility = VISIBLE
            } else {
                currentUserMessageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadMessageImageIntoView(currentUserMessagePhotoImageView, message.image!!, photoProgressBar)
                currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                currentUserMessagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener { root ->
                //only work if action mode is on, we know this when messagesSelectedList is not empty
                //as a result of OnLongClick being already triggered or resetMessageSelectedList
                //hasn't been called
                if(messagesSelectedList.isNotEmpty()) {
                    //a list of selected messages is used to keep track of selected messages to avoid
                    //inconsistency when views are recycled
                    if (messagesSelectedList.contains(message.messageId)) {
                        //remove message id if they have already been selected
                        messagesSelectedList.remove(message.messageId)
                        //set background color to indicate deselection
                        root.setBackgroundResource(R.color.white)
                    } else {
                        //add message id if they haven't be selected
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        //set background color to indicate selection
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    //update list of views
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, root)
                }
            }

            root.setOnLongClickListener {
                //a list of selected messages is used to keep track of selected messages to avoid
                //inconsistency when views are recycled
                if (messagesSelectedList.contains(message.messageId)) {
                    //remove message if they have already been selected
                    messagesSelectedList.remove(message.messageId)
                    //set background color to indicate deselection
                    root.setBackgroundResource(R.color.white)
                } else {
                    //add message if they haven't be selected
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    //set background color to indicate selection
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                //update list of views
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }

            //during onBindViewHolder, which may occur when views are recycled, we use the tracking
            //list of selected messages to keep the background resource of each message's view set
            //to the appropriate color
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }
        }
    }

    inner class GroupMessageViewHolder(private val binding: ItemGroupMessageOtherUserBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage, itemPosition: Int) = with(binding) {

            //view user profile when image is clicked
            messengerImageView.setOnClickListener {
                message.userId?.let { it1 -> clickListener.onUserClicked(it1, it) }
            }

            //load user photo
            Firebase.database.reference.child("photos").child(message.userId!!).get()
                .addOnSuccessListener {  dataSnapshot ->
                    val photo = dataSnapshot.getValue(String::class.java)
                    if (snapshots.size != 0) {
                        if(itemPosition > 0 && snapshots[itemPosition].userId ==
                            snapshots[itemPosition - 1].userId) {
                            messengerImageView.visibility = GONE
                        } else {
                            if (photo != null) {
                                loadUserPhotoIntoView(messengerImageView, photo)
                            } else {
                                Glide.with(context).load(R.drawable.ic_person_light_pearl)
                                    .into(messengerImageView)
                                messengerImageView.visibility = VISIBLE
                            }
                        }
                    }
            }

            //set message body
            if (message.text != null) {
                messageTextView.text = message.text
                messageTextView.visibility = VISIBLE
            } else {
                messageTextView.visibility = GONE
            }

            //set username
            Firebase.database.reference.child("profiles").child(message.userId!!)
                .child("name").get()
                .addOnSuccessListener { dataSnapshot ->
                    val name = dataSnapshot.getValue<String>()
                    if (snapshots.size != 0) {
                        if(itemPosition > 0 && snapshots[itemPosition].userId ==
                            snapshots[itemPosition - 1].userId) {
                            messengerNameTextView.visibility = GONE
                        } else {
                            messengerNameTextView.text = name
                            messengerNameTextView.visibility = VISIBLE
                        }
                    }
                }

            //load message photo if any
            if (message.image != null) {
                loadMessageImageIntoView(messagePhotoImageView, message.image!!, photoProgressBar)
            } else {
                messagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener { root ->
                //only work if action mode is on, we know this when messagesSelectedList is not empty
                //as a result of OnLongClick being already triggered or resetMessageSelectedList
                //hasn't been called
                if(messagesSelectedList.isNotEmpty()) {
                    //a list of selected messages is used to keep track of selected messages to avoid
                    //inconsistency when views are recycled
                    if (messagesSelectedList.contains(message.messageId)) {
                        //remove message id if they have already been selected
                        messagesSelectedList.remove(message.messageId)
                        //set background color to indicate deselection
                        root.setBackgroundResource(R.color.white)
                    } else {
                        //add message id if they haven't be selected
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        //set background color to indicate selection
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    //update list of views
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, root)
                }
            }

            root.setOnLongClickListener {
                //a list of selected messages is used to keep track of selected messages to avoid
                //inconsistency when views are recycled
                if (messagesSelectedList.contains(message.messageId)) {
                    //remove message if they have already been selected
                    messagesSelectedList.remove(message.messageId)
                    //set background color to indicate deselection
                    root.setBackgroundResource(R.color.white)
                } else {
                    //add message if they haven't be selected
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    //set background color to indicate selection
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                //update list of views
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }

            //during onBindViewHolder, which may occur when views are recycled, we use the tracking
            //list of selected messages to keep the background resource of each message's view set
            //to the appropriate color
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
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
        fun bind(message: GroupMessage, itemPosition: Int) = with(binding) {

            //set message body
            if (message.text != null) {
                messageTextView.text = message.text
                messageTextView.visibility = VISIBLE
            } else {
                messageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadMessageImageIntoView(messagePhotoImageView, message.image!!, photoProgressBar)
            } else {
                messagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener { root ->
                //only work if action mode is on, we know this when messagesSelectedList is not empty
                //as a result of OnLongClick being already triggered or resetMessageSelectedList
                //hasn't been called
                if(messagesSelectedList.isNotEmpty()) {
                    //a list of selected messages is used to keep track of selected messages to avoid
                    //inconsistency when views are recycled
                    if (messagesSelectedList.contains(message.messageId)) {
                        //remove message id if they have already been selected
                        messagesSelectedList.remove(message.messageId)
                        //set background color to indicate deselection
                        root.setBackgroundResource(R.color.white)
                    } else {
                        //add message id if they haven't be selected
                        message.messageId?.let { id -> messagesSelectedList.add(id) }
                        //set background color to indicate selection
                        root.setBackgroundResource(R.color.lightest_pearl)
                    }
                    //update list of views
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                    clickListener.onItemClicked(message, root)
                }
            }

            root.setOnLongClickListener {
                //a list of selected messages is used to keep track of selected messages to avoid
                //inconsistency when views are recycled
                if (messagesSelectedList.contains(message.messageId)) {
                    //remove message if they have already been selected
                    messagesSelectedList.remove(message.messageId)
                    //set background color to indicate deselection
                    root.setBackgroundResource(R.color.white)
                } else {
                    //add message if they haven't be selected
                    message.messageId?.let { id -> messagesSelectedList.add(id) }
                    //set background color to indicate selection
                    root.setBackgroundResource(R.color.lightest_pearl)
                }
                //update list of views
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(message, it)
                true
            }

            //during onBindViewHolder, which may occur when views are recycled, we use the tracking
            //list of selected messages to keep the background resource of each message's view set
            //to the appropriate color
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }
        }
    }

    //user photo loader
    private fun loadUserPhotoIntoView(imageView: ShapeableImageView, photoUrl: String) {
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
        } else {
            Glide.with(imageView.context).load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(imageView)
            imageView.visibility = VISIBLE
        }
    }

    //message Image Loader
    private fun loadMessageImageIntoView(imageView: ShapeableImageView, photoUrl: String, photoProgressBar: ProgressBar) {
        if (photoUrl.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(photoUrl)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    val options = RequestOptions()
                        .error(R.drawable.ic_downloading)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    imageView.visibility = VISIBLE
                    //using custom glide image loader to indicate progress in time
                    GlideImageLoader(imageView, photoProgressBar).load(downloadUrl, options)
                }
        } else {
            val options = RequestOptions()
                .error(R.drawable.ic_downloading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

            imageView.visibility = VISIBLE
            //using custom glide image loader to indicate progress in time
            GlideImageLoader(imageView, photoProgressBar).load(photoUrl, options)
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

