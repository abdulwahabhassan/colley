package com.colley.android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.model.Post
import com.colley.android.model.Profile
import com.firebase.ui.database.ObservableSnapshotArray
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.firebase.ui.database.paging.FirebaseRecyclerPagingAdapter
import com.firebase.ui.database.paging.LoadingState

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class PostsRecyclerAdapter(
    private val options: DatabasePagingOptions<Post>,
    private val context: Context,
    private val currentUser: FirebaseUser?,
    private val loadingStateListener: LoadingStateChanged,
//    private val onDataChangedListener: DataChangedListener,
    private val clickListener: ItemClickedListener
)
    : FirebaseRecyclerPagingAdapter<Post, PostsRecyclerAdapter.PostViewHolder>(options) {

//    //listener to hide progress bar and display views only when data has been retrieved from database and bound to view holder
//    interface DataChangedListener {
//        fun onDataAvailable(snapshotArray: ObservableSnapshotArray<Post>)
//    }


    interface LoadingStateChanged {
        fun onLoadingStateChanged(state: LoadingState)
    }

    interface ItemClickedListener {
        fun onItemClick(postId: String, view: View)
        fun onItemLongCLicked(postId: String, view: View)
        fun onUserClicked(userId: String, view: View)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val viewBinding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, model: Post) {
        holder.bind(currentUser, model, context, clickListener)
    }

//    //Callback triggered after all child events in a particular snapshot have been processed.
//    //Useful for batch events, such as removing a loading indicator
//    override fun onDataChanged() {
//        super.onDataChanged()
//
//        //display GroupMessageFragment EditText layout only when data has been bound,
//        //otherwise show progress bar loading
//        onDataChangedListener.onDataAvailable(snapshots)
//    }

    class PostViewHolder (private val itemBinding : ItemPostBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(currentUser: FirebaseUser?, post: Post, context: Context, clickListener: ItemClickedListener) = with(itemBinding) {

            //check if userId is not null
            post.userId?.let { userId ->
                //retrieve user profile
                Firebase.database.reference.child("profiles").child(userId).addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val profile = snapshot.getValue<Profile>()
                            //set the name of user who raised this issue
                            nameTextView.text = profile?.name
                            //set the school of the user who raised this issue
                            schoolTextView.text = profile?.school
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
                                Glide.with(root.context).load(photo).into(userPhotoImageView)
                            } else {
                                Glide.with(root.context).load(R.drawable.ic_person).into(userPhotoImageView)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    }
                )
            }

            post.image?.let {
                //retrieve and set post image if postId isn't null
                post.postId?.let { it1 ->
                    Firebase.database.reference.child("posts").child(it1).child("image").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val image = snapshot.getValue<String>()
                                //set photo
                                if (image != null) {
                                    Glide.with(root.context).load(image).into(contentImageView)
                                } else {
                                    contentImageView.visibility = GONE
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }
                    )
                }
            }

            //set time stamp
            timeStampTextView.text = post.timeStamp

            //dismiss post text view if null else set it
            if(post.text == null) {
                contentTextView.visibility = GONE
            } else {
                contentTextView.text = post.text
            }

            //dismiss post location view if null else set it
            if(post.location == null) {
                locationTextView.visibility = GONE
            } else {
                locationTextView.text = post.location
            }

            when (post.likes) {
                0 -> likeCountTextView.visibility = GONE
                1 -> {
                    likeCountTextView.visibility = View.VISIBLE
                    likeCountTextView.text = "${post.likes} like"
                }
                else -> {
                    likeCountTextView.visibility = View.VISIBLE
                    likeCountTextView.text = "${post.likes} likes"
                }
            }

            when (post.comments) {
                0 -> commentCountTextView.visibility = GONE
                1 -> {
                    commentCountTextView.visibility = View.VISIBLE
                    commentCountTextView.text = "${post.comments} comment"
                }
                else -> {
                    commentCountTextView.visibility = View.VISIBLE
                    commentCountTextView.text = "${post.comments} comments"
                }
            }

            when (post.promotions) {
                0 -> promotionCountTextView.visibility = GONE
                1 -> {
                    promotionCountTextView.visibility = View.VISIBLE
                    promotionCountTextView.text = "${post.promotions} promotion"
                }
                else -> {
                    promotionCountTextView.visibility = View.VISIBLE
                    promotionCountTextView.text = "${post.promotions} promotions"
                }
            }

            root.setOnClickListener {

                if(post.postId != null) {
                    clickListener.onItemClick(post.postId, it)
                }
            }

            root.setOnLongClickListener {
                if(post.postId != null) {
                    clickListener.onItemLongCLicked(post.postId, it)
                }
                true
            }

            nameTextView.setOnClickListener {
                if(post.userId != null) {
                    clickListener.onUserClicked(post.userId, it)
                }
            }

            userPhotoImageView.setOnClickListener {
                if(post.userId != null) {
                    clickListener.onUserClicked(post.userId, it)
                }
            }
        }
    }


//    override fun getItemId(position: Int): Long {
//        return position.toLong()
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return position
//    }

    override fun onLoadingStateChanged(state: LoadingState) {
        loadingStateListener.onLoadingStateChanged(state)
    }


}
