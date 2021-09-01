package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemChatBinding
import com.colley.android.databinding.ItemIssueBinding
import com.colley.android.model.Issue
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

class IssuesRecyclerAdapter(
    private val options: FirebaseRecyclerOptions<Issue>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener
)
    : FirebaseRecyclerAdapter<Issue, IssuesRecyclerAdapter.IssueViewHolder>(options) {

    //listener to hide progress bar and display views only when data has been retrieved from database and bound to view holder
    interface DataChangedListener {
        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<Issue>)
    }

    interface ItemClickedListener {
        fun onItemClick(issueId: String, view: View)
        fun onItemLongCLicked(issueId: String, view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val viewBinding = ItemIssueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IssueViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int, model: Issue) {
        holder.bind(currentUser, model, context, clickListener)
    }

    //Callback triggered after all child events in a particular snapshot have been processed.
    //Useful for batch events, such as removing a loading indicator
    override fun onDataChanged() {
        super.onDataChanged()

        //display GroupMessageFragment EditText layout only when data has been bound,
        //otherwise show progress bar loading
        onDataChangedListener.onDataAvailable(snapshots)
    }

    class IssueViewHolder (private val itemBinding : ItemIssueBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(currentUser: FirebaseUser?, issue: Issue, context: Context, clickListener: ItemClickedListener) = with(itemBinding) {

            //check if userId is not null
            issue.userId?.let { userId ->
                //retrieve user profile
                Firebase.database.reference.child("profiles").child(userId).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val profile = snapshot.getValue<Profile>()
                            //set the name of user who raised this issue
                            userNameTextView.text = profile?.name
                            //set the school of the user who raised this issue
                            userSchoolTextView.text = profile?.school
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }
                )

                //retrieve user photo
                Firebase.database.reference.child("photos").child(userId).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val photo = snapshot.getValue<String>()
                            //set photo
                            if (photo != null) {
                                Glide.with(root.context).load(photo).into(userImageView)
                            } else {
                                Glide.with(root.context).load(R.drawable.ic_profile).into(userImageView)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }
                )
            }

            //set issue title, body and timeStamp
            issueTitleTextView.text = issue.title
            issueBodyTextView.text = issue.body
            issueTimeStampTextView.text = issue.timeStamp

            //check if issue id is not null before querying the issue
            if (issue.issueId != null) {

                //add listener to contribution count to update on value change
                Firebase.database.reference.child("issues").child(issue.issueId)
                    .child("contributions").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val contribution = snapshot.getValue<Int>()
                                //if contribution count is not null and 0
                                if (contribution != null) {
                                    //set contribution
                                    contributionsTextView.text = contribution.toString()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }
                    )

                //add listener to endorsement count to update on value change
                Firebase.database.reference.child("issues").child(issue.issueId)
                    .child("endorsement").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val endorsement = snapshot.getValue<Int>()
                                //if endorsement count is not null and 0
                                if (endorsement != null) {
                                    //set endorsement
                                    endorsementTextView.text = endorsement.toString()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {} }
                    )
            }


//            pinImageView.setOnClickListener {
//                it.isActivated = when (it.isActivated) {
//                    true -> false
//                    false -> true
//                }
//                if (it.isActivated) {
//                    Toast.makeText(root.context, "pinned", Toast.LENGTH_SHORT).show()
//                }
//            }

            root.setOnClickListener {

                if(issue.issueId != null) {
                    clickListener.onItemClick(issue.issueId, it)
                }
            }

            root.setOnLongClickListener {
                if(issue.issueId != null) {
                    clickListener.onItemLongCLicked(issue.issueId, it)
                }
                true
            }
        }
    }


}
