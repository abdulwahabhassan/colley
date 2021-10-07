package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemWhoToMessageBinding
import com.colley.android.model.Profile
import com.colley.android.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class WhoToMessageRecyclerAdapter(
    private val clickListener: ItemClickedListener,
    private val context: Context,
    private val listOfUsers: ArrayList<User>
) :
    RecyclerView.Adapter<WhoToMessageRecyclerAdapter.UserViewHolder>() {


    interface ItemClickedListener {
        fun onItemClick(user: User)
    }

    inner class UserViewHolder (private val itemBinding : ItemWhoToMessageBinding)
        : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(user: User, clickListener: ItemClickedListener, context: Context) =
            with(itemBinding) {

            //set name
            Firebase.database.reference.child("profiles").child(user.userId!!)
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue<Profile>()?.name
                        userNameTextView.text = name
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )

            //load photo
            Firebase.database.reference.child("photos").child(user.userId!!)
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val photo = snapshot.getValue<String>()
                        if (photo != null) {
                            Glide.with(context).load(photo).into(userImageView)
                        } else {
                            Glide.with(context).load(R.drawable.ic_person).into(userImageView)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val viewBinding = ItemWhoToMessageBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val member = listOfUsers[position]
        holder.bind(member, clickListener, context)
    }

    override fun getItemCount(): Int {
        return listOfUsers.size
    }

}