package com.colley.android.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.allViews
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.colley.android.R
import com.colley.android.databinding.*
import com.colley.android.glide.GlideImageLoader
import com.colley.android.model.PrivateMessage
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PrivateMessageRecyclerAdapter(
    options: FirebaseRecyclerOptions<PrivateMessage>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val onDataChangedListener: DataChangedListener,
    private val context: Context
) : FirebaseRecyclerAdapter<PrivateMessage, RecyclerView.ViewHolder>(options) {

    //list to keep tracked of selected messages
    private var messagesSelectedList = arrayListOf<String>()
    //list to keep tracked of selected views
    private var viewsSelectedList = arrayListOf<View>()

    //function to clear selected messages tracking list when action mode is off
    fun resetMessagesSelectedList() {
        messagesSelectedList.clear()
    }
    //rest back resources for all seleceted views when action mode is destroyed
    fun restBackgroundForSelectedViews() {
        viewsSelectedList.forEach { view ->
            Log.w("viewsRest", "$view" )
            view.setBackgroundResource(R.color.white)
        }
        viewsSelectedList.clear()
    }

    //listener to hide progress bar and display views only when data has been retrieved from
    //database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateMessage>)
    }

    interface ItemClickedListener {
        fun onItemLongCLicked(message: PrivateMessage, view: View)
        fun onItemClicked(message: PrivateMessage, view: View)
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
        model: PrivateMessage
    ) {

        val uid = currentUser?.uid

        if (snapshots[position].fromUserId != uid) {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
                    (holder as OtherUserPrivateMessageViewHolderSame).bind(model, position)
                } else {
                    (holder as OtherUserPrivateMessageViewHolder).bind(model, position)
                }
            }

        } else {
            if (snapshots.size != 0) {
                if(position > 0 && snapshots[position].fromUserId ==
                    snapshots[position - 1].fromUserId) {
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
        private val itemBinding: ItemPrivateMessageCurrentUserBinding
    )
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with(itemBinding) {

            //set message body
            if (message.text != null) {
                currentUserMessageTextView.text = message.text
                currentUserMessageTextView.visibility = VISIBLE
            } else {
                currentUserMessageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadImageIntoView(currentUserMessagePhotoImageView, message.image!!, photoProgressBar)
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

    inner class CurrentUserPrivateMessageViewHolderSame(
        private val itemBinding: ItemPrivateMessageCurrentUserSameBinding
    ) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with (itemBinding) {

            //set message body
            if (message.text != null) {
                currentUserMessageTextView.text = message.text
                currentUserMessageTextView.visibility = VISIBLE
            } else {
                currentUserMessageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadImageIntoView(
                    itemBinding.currentUserMessagePhotoImageView,
                    message.image!!,
                    photoProgressBar)
                currentUserMessagePhotoImageView.visibility = VISIBLE
            } else {
                currentUserMessagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener {
                //only work if action mode is on, we know this when messagesSelectedList is not
                //empty as a result of OnLongClick being already triggered or
                //resetMessageSelectedList hasn't been called
                if(messagesSelectedList.isNotEmpty()) {
                    //a list of selected messages is used to keep track of selected messages to
                    //avoid inconsistency when views are recycled
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
                    clickListener.onItemClicked(message, it)
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

    inner class OtherUserPrivateMessageViewHolder(
        private val itemBinding: ItemPrivateMessageOtherUserBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with(itemBinding) {

            //set message body
            if (message.text != null) {
                messageTextView.text = message.text
                itemBinding.messageTextView.visibility = VISIBLE
            } else {
                messageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadImageIntoView(messagePhotoImageView, message.image!!, photoProgressBar)
                messagePhotoImageView.visibility = VISIBLE
            } else {
                messagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener {
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
                    clickListener.onItemClicked(message, it)
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
            //during onBindViewHolder, which may occur when views are recycled, we use the tracking list
            //of selected messages to keep the background resource of each message's view set
            //to the appropriate color
            if(messagesSelectedList.contains(message.messageId)) {
                root.setBackgroundResource(R.color.lightest_pearl)
            } else {
                root.setBackgroundResource(R.color.white)
            }

        }

    }

    inner class OtherUserPrivateMessageViewHolderSame(
        private val itemBinding: ItemPrivateMessageOtherUserSameBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(message: PrivateMessage, itemPosition: Int) = with(itemBinding) {

            //set message body
            if (message.text != null) {
                messageTextView.text = message.text
                messageTextView.visibility = VISIBLE
            } else {
                messageTextView.visibility = GONE
            }

            //load message photo if any
            if (message.image != null) {
                loadImageIntoView(messagePhotoImageView, message.image!!, photoProgressBar)
                messagePhotoImageView.visibility = VISIBLE
            } else {
                messagePhotoImageView.visibility = GONE
                photoProgressBar.visibility = GONE
            }

            root.setOnClickListener {
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
                    clickListener.onItemClicked(message, it)
                }
            }

            root.setOnLongClickListener { root ->
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
                clickListener.onItemLongCLicked(message, root)
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

    //image loader
    private fun loadImageIntoView(
        imageView: ShapeableImageView,
        photoUrl: String,
        photoProgressBar: ProgressBar) {

        photoProgressBar.visibility = VISIBLE
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
                    GlideImageLoader(imageView, photoProgressBar).load(downloadUrl, options);
                }

        } else {

            val options = RequestOptions()
                .error(R.drawable.ic_downloading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

            imageView.visibility = VISIBLE
            //using custom glide image loader to indicate progress in time
            GlideImageLoader(imageView, photoProgressBar).load(photoUrl, options);
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

