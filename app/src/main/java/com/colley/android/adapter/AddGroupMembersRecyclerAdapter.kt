package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.ItemNewGroupMemberBinding
import com.colley.android.model.Profile
import com.colley.android.model.User
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddGroupMembersRecyclerAdapter(
    options: FirebaseRecyclerOptions<User>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val context: Context
) :
    FirebaseRecyclerAdapter<User, AddGroupMembersRecyclerAdapter.GroupMemberViewHolder>(options) {

    //list to keep tracked of selected members to add to group
    private var memberSelectedList = arrayListOf<String>()

    interface ItemClickedListener {
        fun onItemClick(user: User)
        fun onItemSelected(userId: String, view: CheckBox)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemNewGroupMemberBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int, model: User) {
        holder.bind(model, clickListener, context)
    }

    //enforces unique view type for each item
    override fun getItemViewType(position: Int): Int {
        return position
    }


    inner class GroupMemberViewHolder (private val itemBinding : ItemNewGroupMemberBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(user: User, clickListener: ItemClickedListener, context: Context) =
            with(itemBinding) {

            //check is user is the current user
            if(user.userId == currentUser?.uid) {
                //if yes, check by default and disable checkbox
                addGroupMemberCheckBox.isChecked = true
                addGroupMemberCheckBox.isEnabled = false
                //add user to tracking list if they haven't already been added
                if (!memberSelectedList.contains(user.userId)) {
                    //add current user to check tracking list
                    user.userId?.let { memberSelectedList.add(it) }
                }
            } else {
                //set on click listeners to checkbox of members who aren't a member already
                addGroupMemberCheckBox.setOnClickListener {

                    //get reference to clicked user's id
                    val clickedUserId = user.userId

                    //a list of selected members is used to keep track of selected users when views
                    //are recycled
                    if (memberSelectedList.contains(clickedUserId)) {
                        //remove a user if they have already been selected
                        memberSelectedList.remove(clickedUserId)
                    } else {
                        //add a user if they haven't be selected
                        memberSelectedList.add(clickedUserId!!)
                    }

                    //clickListener to count and display selected users
                    clickListener.onItemSelected(user.userId!!, it as CheckBox)

                }
            }

            //during onBindViewHolder, which may occur when views are recycled, we use the tracking
            //list of selected user to keep checkbox status consistent
            addGroupMemberCheckBox.isChecked = memberSelectedList.contains(user.userId)

            //set name
            Firebase.database.reference.child("profiles").child(user.userId!!)
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue<Profile>()?.name

                        //if current user is this user, append (You) else only display their name
                        if (currentUser?.uid == user.userId) {
                            groupMemberNameTextView.text = "$name (You)"
                        } else {
                            groupMemberNameTextView.text = name
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            //load photo
            Firebase.database.reference.child("photos").child(user.userId!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String>()
                        if (photo != null) {
                            Glide.with(context).load(photo)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .into(groupMemberImageView)
                        } else {
                            Glide.with(context).load(R.drawable.ic_person_light_pearl)
                                .into(groupMemberImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            this.root.setOnClickListener {
                clickListener.onItemClick(user)
            }
        }
    }

}