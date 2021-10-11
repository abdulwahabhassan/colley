package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.ItemGroupMemberBinding
import com.colley.android.model.Profile
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class GroupMembersRecyclerAdapter(
    options: FirebaseRecyclerOptions<String>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val context: Context,
    private val groupId: String
        ) : FirebaseRecyclerAdapter<String, GroupMemberViewHolder>(options) {

    interface ItemClickedListener {
        fun onItemClick(memberId: String)
        fun onItemLongCLicked(memberId: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemGroupMemberBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding, currentUser, groupId, context)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int, model: String) {
        holder.bind(model, clickListener)
    }

    //enforces unique view type for each item
    override fun getItemViewType(position: Int): Int {
        return position
    }
}

class GroupMemberViewHolder(
    private val itemBinding: ItemGroupMemberBinding,
    private val currentUser: FirebaseUser?,
    private val groupId: String,
    private val context: Context
) : RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(memberId: String, clickListener: GroupMembersRecyclerAdapter.ItemClickedListener) =
        with (itemBinding) {

        //indicate admins
        Firebase.database.reference.child("groups").child(groupId)
            .child("groupAdmins").addListenerForSingleValueEvent(
             object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val admins = snapshot.getValue<ArrayList<String>>()
                    if(admins?.contains(memberId) == true) {
                        adminTextView.visibility = VISIBLE
                    } else {
                        adminTextView.visibility = GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )

        //set member name
        Firebase.database.reference.child("profiles").child(memberId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue<Profile>()

                    //if member id is the current user Id, append you
                    if(memberId == currentUser?.uid) {
                        groupMemberNameTextView.text = profile?.name + " (You)"
                    } else {
                        groupMemberNameTextView.text = profile?.name
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )

        //load member photo
        Firebase.database.reference.child("photos").child(memberId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val photoUrl = snapshot.getValue<String>()
                    if (photoUrl != null) {
                        Glide.with(context).load(photoUrl).diskCacheStrategy(
                            DiskCacheStrategy.RESOURCE).into(groupMemberImageView)
                    } else {
                        Glide.with(context).load(R.drawable.ic_person_light_pearl)
                            .into(groupMemberImageView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )


        //on long click
        root.setOnLongClickListener {
            clickListener.onItemLongCLicked(memberId)
            true
        }

        //on click
        root.setOnClickListener {
            clickListener.onItemClick(memberId)
        }

    }
}