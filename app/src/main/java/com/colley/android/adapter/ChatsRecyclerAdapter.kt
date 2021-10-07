package com.colley.android.adapter

import android.annotation.SuppressLint
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
import com.colley.android.databinding.ItemChatBinding
import com.colley.android.model.PrivateChat
import com.colley.android.model.Profile
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class ChatsRecyclerAdapter(
    options: FirebaseRecyclerOptions<PrivateChat>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener
)
    : FirebaseRecyclerAdapter<PrivateChat, ChatsRecyclerAdapter.PrivateMessageViewHolder>(options) {

    //list to keep tracked of selected chats
    private var chatsSelectedList = arrayListOf<String>()
    //list to keep tracked of selected views
    private var viewsSelectedList = arrayListOf<View>()

    //function to clear selected chats tracking list when action mode is off
    fun resetSelectedChatsList() {
        chatsSelectedList.clear()
    }
    //rest back resources for all seleceted views when action mode is destroyed
    fun restBackgroundOfSelectedViews() {

        viewsSelectedList.forEach { view ->
            view.setBackgroundResource(R.color.white)
        }
        viewsSelectedList.clear()
    }

    //listener to hide progress bar and display views only when data has been retrieved from
    //database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateChat>)
    }

    interface ItemClickedListener {
        fun onItemClick(chateeId: String, view: View)
        fun onItemLongCLicked(chateeId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateMessageViewHolder {
        val viewBinding = ItemChatBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PrivateMessageViewHolder(viewBinding)
    }

    override fun onBindViewHolder(
        holder: PrivateMessageViewHolder,
        position: Int,
        model: PrivateChat) {
        holder.bind(currentUser, model, context, clickListener)
    }

    //return unique view type for each item
    override fun getItemViewType(position: Int): Int {
        return position
    }

    //Callback triggered after all child events in a particular snapshot have been processed.
    //Useful for batch events, such as removing a loading indicator
    override fun onDataChanged() {
        super.onDataChanged()
        //display GroupMessageFragment EditText layout only when data has been bound,
        //otherwise show progress bar loading
        onDataChangedListener.onDataAvailable(snapshots)
    }

    inner class PrivateMessageViewHolder (private val itemBinding : ItemChatBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            currentUser: FirebaseUser?,
            chat: PrivateChat,
            context: Context,
            clickListener: ItemClickedListener) = with(itemBinding) {

            //check which id rightly belongs to the chatee
            val chateeId = if (chat.fromUserId == currentUser?.uid) chat.toUserId as String
                            else chat.fromUserId as String

            //get and set chatee name
            Firebase.database.reference.child("profiles").child(chateeId)
                .child("name").get().addOnSuccessListener { dataSnapshot ->
                val name = dataSnapshot.getValue(String::class.java)
                    if(name != null) {
                        personNameTextView.text = name
                    } else {
                        personNameTextView.text = "Unknown user"
                    }
            }

            //set recent message text if not null and hide imageview
            if(chat.text != null) {
                recentMessageImageView.visibility = GONE
                recentMessageTextView.text = chat.text
                recentMessageTextView.visibility  = VISIBLE
            }

            //set recent message as "photo image" if it is a photo and hide text view
            if (chat.image != null) {
                recentMessageTextView.visibility  = GONE
                recentMessageImageView.visibility = VISIBLE
            }

            //set chatee photo
            Firebase.database.reference.child("photos").child(chateeId).get()
                .addOnSuccessListener { dataSnapshot ->
                    val photo = dataSnapshot.getValue<String>()
                    if (photo != null) {
                        Glide.with(context).load(photo).diskCacheStrategy(
                        DiskCacheStrategy.RESOURCE).into(personImageView)
                } else {
                        Glide.with(context).load(R.drawable.ic_person_light_pearl)
                            .into(personImageView)
                    }

            }

            root.setOnClickListener { root ->
                //only work if action mode is on, we know this when chatsSelectedList is not empty
                //as a result of OnLongClick being already triggered or resetChatsSelectedList
                //hasn't been called
                if (chatsSelectedList.isNotEmpty()) {
                    //a list of selected chats is used to keep track of selected chats to avoid
                    //inconsistency when views are recycled
                    if (chatsSelectedList.contains(chateeId)) {
                        //remove chatee id if they have already been selected
                        chatsSelectedList.remove(chateeId)
                        //set background color to indicate deselection
                        root.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
                    } else {
                        //add chatee id if they haven't already been selected
                        chatsSelectedList.add(chateeId)
                        //set background color to indicate selection
                        root.setBackgroundResource(R.drawable.selected_chat_background)
                    }
                    //update list of views
                    if (viewsSelectedList.contains(root)){
                        viewsSelectedList.remove(root)
                    } else {
                        viewsSelectedList.add(root)
                    }
                }
                clickListener.onItemClick(chateeId, root)
            }

            root.setOnLongClickListener { root ->
                //a list of selected chats is used to keep track of selected chats to avoid
                //inconsistency when views are recycled
                if (chatsSelectedList.contains(chateeId)) {
                    //remove chatee id if they have already been selected
                    chatsSelectedList.remove(chateeId)
                    //set background color to indicate deselection
                    root.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
                } else {
                    //add chatee id if they haven't already been selected
                    chatsSelectedList.add(chateeId)
                    //set background color to indicate selection
                    root.setBackgroundResource(R.drawable.selected_chat_background)
                }
                //update list of views
                if (viewsSelectedList.contains(root)){
                    viewsSelectedList.remove(root)
                } else {
                    viewsSelectedList.add(root)
                }
                clickListener.onItemLongCLicked(chateeId, root)
                true
            }
        }

    }

}
