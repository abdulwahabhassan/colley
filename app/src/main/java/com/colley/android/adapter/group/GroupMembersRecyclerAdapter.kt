package com.colley.android.adapter.group

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemGroupMemberBinding
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

class GroupMembersRecyclerAdapter(
        private val options: FirebaseRecyclerOptions<String>,
    private val currentUser: FirebaseUser?,
    private val clickListener: ItemClickedListener,
    private val context: Context
        ) : FirebaseRecyclerAdapter<String, GroupMembersRecyclerAdapter.GroupMemberViewHolder>(options) {

    interface ItemClickedListener {
        fun onItemClick(memberId: String)
    }

            inner class GroupMemberViewHolder(private val itemBinding: ItemGroupMemberBinding)
                : RecyclerView.ViewHolder(itemBinding.root) {
                fun bind(memberId: String, clickListener: ItemClickedListener) = with (itemBinding) {

                    //set member name
                    Firebase.database.reference.child("profiles").child(memberId).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val profile = snapshot.getValue<Profile>()
                                groupMemberNameTextView.text = profile?.name
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.w(TAG, "getMemberName:OnCancelled", error.toException())
                            }
                        }
                    )

                    //load member photo
                    Firebase.database.reference.child("photos").child(memberId).addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val photoUrl = snapshot.getValue<String>()
                                if (photoUrl != null) {
                                    Glide.with(context).load(photoUrl).into(groupMemberImageView)
                                } else {
                                    Glide.with(context).load(R.drawable.ic_person).into(groupMemberImageView)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.w(TAG, "getMemberPhoto:OnCancelled", error.toException())
                            }
                        }
                    )

                    root.setOnClickListener {
                        clickListener.onItemClick(memberId)
                    }

                }
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val viewBinding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupMemberViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int, model: String) {
        holder.bind(model, clickListener)
    }

    companion object {
        const val TAG = "groupMembers"
    }
}