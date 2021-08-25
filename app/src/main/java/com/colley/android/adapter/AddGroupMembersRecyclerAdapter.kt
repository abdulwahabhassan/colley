package com.colley.android.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemNewGroupMemberBinding
import com.colley.android.model.Profile
import com.colley.android.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddGroupMembersRecyclerAdapter(
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val context: Context,
    private val listOfMembers: ArrayList<User>
) :
    RecyclerView.Adapter<AddGroupMembersRecyclerAdapter.GroupMemberViewHolder>() {

    //list to keep tracked of selected members to add to group
    private var memberSelectedList = arrayListOf<String>()

    interface ItemClickedListener {
        fun onItemClick(user: User)
        fun onItemSelected(userId: String, view: CheckBox)
    }

    inner class GroupMemberViewHolder (private val itemBinding : ItemNewGroupMemberBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(user: User, clickListener: ItemClickedListener, context: Context) = with(itemBinding) {


            addGroupMemberCheckBox.setOnClickListener {

                //get reference to clicked user's id
                val clickedUserId = user.userId

                //a list of selected members is used to keep track of selected users when views are recycled
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
            //during onBindViewHolder, which may occur when views are recycled, we use the tracking list
            //of selected user to keep checkbox status consistent
            addGroupMemberCheckBox.isChecked = memberSelectedList.contains(user.userId)

            //set name
            Firebase.database.reference.child("profiles").child(user.userId!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue<Profile>()?.name
                        groupMemberNameTextView.text = name

                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getUserName:OnCancelled", error.toException())
                    }
                }
            )

            //load photo
            Firebase.database.reference.child("photos").child(user.userId!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String>()
                        if (photo != null) {
                            Glide.with(context).load(photo).into(groupMemberImageView)
                        } else {
                            Glide.with(context).load(R.drawable.ic_person).into(groupMemberImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getUserPhoto:OnCancelled", error.toException())
                    }
                }
            )

            this.root.setOnClickListener {
                clickListener.onItemClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemNewGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int) {
        val member = listOfMembers[position]
        holder.bind(member, clickListener, context)
    }

    override fun getItemCount(): Int {
        return listOfMembers.size
    }

    companion object {
        const val TAG = "AGMRecyclerAdapter"
    }

}