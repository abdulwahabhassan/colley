package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemChatBinding
import com.colley.android.model.PrivateChat
import com.colley.android.model.Profile
import com.colley.android.view.fragment.ChatsFragment
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
    private val options: FirebaseRecyclerOptions<PrivateChat>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener
)
    : FirebaseRecyclerAdapter<PrivateChat, ChatsRecyclerAdapter.PrivateMessageViewHolder>(options) {

    //listener to hide progress bar and display views only when data has been retrieved from database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateChat>)
    }

    interface ItemClickedListener {
        fun onItemClick(chateeId: String, view: View)
        fun onItemLongCLicked(chateeId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateMessageViewHolder {
        val viewBinding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PrivateMessageViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: PrivateMessageViewHolder, position: Int, model: PrivateChat) {
        holder.bind(currentUser, model, context, clickListener)
        //display chat groups only when data has been bound,
        //otherwise show progress bar loading
    }

    //Callback triggered after all child events in a particular snapshot have been processed.
    //Useful for batch events, such as removing a loading indicator
    override fun onDataChanged() {
        super.onDataChanged()

        //display GroupMessageFragment EditText layout only when data has been bound,
        //otherwise show progress bar loading
        onDataChangedListener.onDataAvailable(snapshots)
    }

    class PrivateMessageViewHolder (private val itemBinding : ItemChatBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(currentUser: FirebaseUser?, chat: PrivateChat, context: Context, clickListener: ItemClickedListener) = with(itemBinding) {

            //check which id rightly belongs to the chatee
            val chateeId = if (chat.fromUserId == currentUser?.uid) chat.toUserId as String else chat.fromUserId as String

            Firebase.database.reference.child("user-messages").child("recent-message").child(currentUser?.uid!!)
            //add a listener to retrieve chatee profile
            Firebase.database.reference.child("profiles").child(chateeId).addListenerForSingleValueEvent(
                object : ValueEventListener {

                    //set chatee name
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chateeProfile = snapshot.getValue<Profile>()
                        if(chateeProfile != null) {
                            personNameTextView.text = chateeProfile.name
                        } else {
                            personNameTextView.text = "Unknown user"
                        }

                        //set recent message
                        if(chat.text != null) {
                            recentMessageTextView.text = chat.text
                        }

                        //set recent message as "photo image" if it is a photo
                        if (chat.image != null) {
                            recentMessageTextView.text = "Sent Image"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            //load chatee profile photo
            Firebase.database.reference.child("photos").child(chateeId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String>()
                        if (photo != null) {
                            Glide.with(context).load(photo).into(this@with.personImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )



            //set chatee photo
            Firebase.database.reference.child("photos").child(chateeId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val chateePhoto = snapshot.getValue<String>()
                        if (chateePhoto != null) {
                            Glide.with(context).load(chateePhoto).into(this@with.personImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            root.setOnClickListener {

                clickListener.onItemClick(chateeId, it)
            }

            root.setOnLongClickListener {
                clickListener.onItemLongCLicked(chateeId, it)
                true
            }
        }
    }


}
