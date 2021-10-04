package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemNotificationCommentBinding
import com.colley.android.databinding.ItemNotificationLikeBinding
import com.colley.android.model.Issue
import com.colley.android.model.Profile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference

class NotificationsPagingAdapter(
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val clickListener: NotificationPagingItemClickedListener,
    private val dbRef: DatabaseReference
)
    : PagingDataAdapter<DataSnapshot, RecyclerView.ViewHolder>(NOTIFICATION_COMPARATOR) {

    interface NotificationPagingItemClickedListener {
        fun onItemClick(notification: com.colley.android.model.Notification)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        //return viewholder depending on viewType specified
        var viewHolder : RecyclerView.ViewHolder? = null

        when(viewType) {
            VIEW_TYPE_LIKE -> {
                val view = inflater.inflate(
                    R.layout.item_notification_like,
                    parent,
                    false)
                val binding = ItemNotificationLikeBinding.bind(view)
                viewHolder = NotificationLikeViewHolder(binding)
            }
            VIEW_TYPE_COMMENT -> {
                val view = inflater.inflate(
                    R.layout.item_notification_comment,
                    parent,
                    false)
                val binding = ItemNotificationCommentBinding.bind(view)
                viewHolder = NotificationCommentViewHolder(binding)
            }
        }
        return viewHolder!!

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        //parse dataSnapshot as Notification
        val notification = getItem(position)?.getValue(com.colley.android.model.Notification::class.java)
        //confirm the type of action (whether like or comment) in order to call the appropriate bind
        //operation
        if (notification?.itemActionType == "like") {
            (holder as NotificationLikeViewHolder)
                .bind(currentUser, notification, context, clickListener)
        } else if (notification?.itemActionType == "comment") {
            (holder as NotificationCommentViewHolder)
                .bind(currentUser, notification, context, clickListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        //deduce and return the appropriate view type for view holder
        var viewType = 0
        //parse dataSnapshot as Notification
        val notification = getItem(position)?.getValue(com.colley.android.model.Notification::class.java)
        if (notification?.itemActionType == "like") {
            viewType = VIEW_TYPE_LIKE
        } else if (notification?.itemActionType == "comment") {
            viewType = VIEW_TYPE_COMMENT
        }
        return viewType
    }

    inner class NotificationCommentViewHolder(private val itemBinding: ItemNotificationCommentBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(currentUser: FirebaseUser?, notification: com.colley.android.model.Notification, context: Context, clickListener: NotificationPagingItemClickedListener) = with(itemBinding) {

            //set notification background depending on whether notification has been viewed by
            //clicking or not
            root.isActivated = notification.clicked

            //set time stamp
            itemBinding.notificationTimeStampTextView.text = notification.timeStamp
            //get user name and school
            notification.itemActorUserId?.let { itemActorUserId ->
                dbRef.child("profiles").child(itemActorUserId).get().addOnSuccessListener {
                    profileSnapshot ->
                    val profile = profileSnapshot.getValue(Profile::class.java)
                    //if the item that was commented on is a post or a issue, do accordingly
                    if(notification.itemType == "post") {
                        itemBinding.notificationTextView.text = "${profile?.name} from ${profile?.school} " +
                                "commented on your post"
                    } else if (notification.itemType == "issue") {
                        notification.itemId?.let { itemId ->
                            //get issue
                            dbRef.child("issues").child(itemId).get().addOnSuccessListener {
                                issueSnapshot ->
                                //retrieve it's title and concatenate it to the notification text
                                val issue = issueSnapshot.getValue(Issue::class.java)
                                itemBinding.notificationTextView.text = "${profile?.name} from ${profile?.school} " +
                                        "commented on your issue: ${issue?.title}"
                            }
                        }
                    }

                }
            }
            //get and load item actor photo
            notification.itemActorUserId?.let { itemActorUserId ->
                dbRef.child("photos").child(itemActorUserId).get().addOnSuccessListener {
                    userPhotoSnapshot ->
                    val photoUrl = userPhotoSnapshot.getValue(String::class.java)
                    if(photoUrl != null) {
                        Glide.with(context).load(photoUrl).into(notificationImageView)
                    } else {
                        Glide.with(context).load(R.drawable.ic_person).into(notificationImageView)
                    }

                }
            }

            root.setOnClickListener {
                //navigate user to post or issue and set notification clicked to true to indicate
                //that notification has been viewed, hence background of notification view should become
                //no longer lightest pearl but white
                clickListener.onItemClick(notification)
            }
        }
    }

    inner class NotificationLikeViewHolder(private val itemBinding: ItemNotificationLikeBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(currentUser: FirebaseUser?, notification: com.colley.android.model.Notification, context: Context, clickListener: NotificationPagingItemClickedListener) = with(itemBinding) {

            //set notification background depending on whether notification has been viewed by
            //clicking or not
            root.isActivated = notification.clicked

            //set time stamp
            itemBinding.notificationTimeStampTextView.text = notification.timeStamp
            //get user name and school
            notification.itemActorUserId?.let { itemActorUserId ->
                dbRef.child("profiles").child(itemActorUserId).get().addOnSuccessListener {
                        profileSnapshot ->
                    val profile = profileSnapshot.getValue(Profile::class.java)
                    //if the item that was liked is a post or a issue, do accordingly
                    if(notification.itemType == "post") {
                        itemBinding.notificationTextView.text = "${profile?.name} from ${profile?.school} " +
                                "liked your post"
                    } else if (notification.itemType == "issue") {
                        notification.itemId?.let { itemId ->
                            //get issue
                            dbRef.child("issues").child(itemId).get().addOnSuccessListener {
                                    issueSnapshot ->
                                //retrieve it's title and concatenate it to the notification text
                                val issue = issueSnapshot.getValue(Issue::class.java)
                                itemBinding.notificationTextView.text = "${profile?.name} from ${profile?.school} " +
                                        "liked your issue: ${issue?.title}"
                            }
                        }
                    }

                }
            }

            root.setOnClickListener {
                //navigate user to post or issue and set notification clicked to true to indicate
                //that notification has been viewed, hence background of notification view should become
                //no longer lightest pearl but white
                clickListener.onItemClick(notification)
            }
        }
    }

    companion object {
        private val NOTIFICATION_COMPARATOR = object : DiffUtil.ItemCallback<DataSnapshot>() {
            override fun areItemsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(com.colley.android.model.Notification::class.java)?.notificationId == newItem.getValue(com.colley.android.model.Notification::class.java)?.notificationId
            }

            override fun areContentsTheSame(
                oldItem: DataSnapshot,
                newItem: DataSnapshot
            ): Boolean {
                return oldItem.getValue(com.colley.android.model.Notification::class.java) == newItem.getValue(com.colley.android.model.Notification::class.java)
            }

        }

        //two view types
        var VIEW_TYPE_LIKE = 0
        var VIEW_TYPE_COMMENT = 1
    }
}