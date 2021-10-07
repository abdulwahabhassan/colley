package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.adapter.PostCommentsPagingAdapter
import com.colley.android.databinding.FragmentCommentsBinding
import com.colley.android.factory.ViewModelFactory
import com.colley.android.model.Comment
import com.colley.android.repository.DatabaseRepository
import com.colley.android.viewmodel.PostCommentsViewModel
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PostCommentsFragment(
    private val postId: String?,
    private val parentContext: Context,
    private val postView: View
) : Fragment(),
    PostCommentsPagingAdapter.PostCommentItemClickedListener {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var commentsCount: Int = 0
    private var differenceCount: Int = 0
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var commentsAdapter: PostCommentsPagingAdapter? = null
    private var manager: WrapContentLinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid

    //value event listener for commentsCount
    private val commentsCountValueEventListener = object : ValueEventListener {
        @SuppressLint("SetTextI18n")
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue(Int::class.java)
            if(count != null) {
                differenceCount = count - commentsCount
            }
            //ignore changes due to adding value event listener to database first time or
            //initial paging load. we know this when current comments count is equal to the difference
            //between the previous count (initial count will be zero) and the current count
            if(differenceCount > 0 && count != differenceCount) {
                if(differenceCount == 1) {
                    binding.newCommentNotificationTextView.text = "^ $differenceCount new comment"
                } else {
                    binding.newCommentNotificationTextView.text = "^ $differenceCount new comments"
                }
                binding.newCommentNotificationTextView.visibility = View.VISIBLE
                return

            } else if(differenceCount > 0) {
                //acknowledge changes due to adding listener first time / initial paging load and
                //only when comments count increases, so that if initially, comments count was zero
                //and then increases, we also want to inform the user if they want to refresh,
                //that a new like has occurred after initial page load and adding value event
                //listener to database first time even though the difference between the current
                //comments count and the previous comments count equals (suggesting either initial page
                //load or adding value event listener to database first time, which in fact is not
                //the case since both events have already occurred)
                if(differenceCount == 1) {
                    binding.newCommentNotificationTextView.text = "$differenceCount comment"
                } else {
                    binding.newCommentNotificationTextView.text = "$differenceCount comments"
                }
                binding.newCommentNotificationTextView.visibility = View.VISIBLE
                return
            } else {
                //if no change in likes count or comments count decreases, it's not important to show it
                binding.newCommentNotificationTextView.visibility = View.INVISIBLE
            }

        }

        override fun onCancelled(error: DatabaseError) {}
    }

    //observer for adapter item changes
    private val commentsAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(positionStart)
            binding.newCommentNotificationTextView.visibility = View.INVISIBLE
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        recyclerView = binding.commentsRecyclerView
        swipeRefreshLayout = binding.postCommentsSwipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!

        //get the view model
        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(PostCommentsViewModel::class.java)

        //get a query reference to issue comments ordered by time code so that the most recent
        //comments appear first
        val commentsQuery = postId?.let { dbRef.child("post-comments").child(it)
            .orderByChild("timeId") }

        //initialize adapter
        commentsAdapter = PostCommentsPagingAdapter(requireContext(), currentUser, this)

        //retrieve number of comments from database to be used for estimating the number of new
        //comments added since the user last refreshed
        getCommentsCount()

        //set recycler view layout manager
        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = commentsAdapter

        //refresh adapter everytime refresh action is called on swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            commentsAdapter?.refresh()
            //reset posts count on refresh so that this fragment knows the correct database posts
            // count
            getCommentsCount()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPostComments(commentsQuery!!).collectLatest {
                    pagingData ->
                commentsAdapter?.submitData(pagingData)
            }
        }

        //Perform some action every time data changes or when there is an error.
        viewLifecycleOwner.lifecycleScope.launch {
            commentsAdapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching comments! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noCommentsLayout.visibility = View.VISIBLE
                        commentsAdapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        //The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        if (commentsAdapter?.itemCount == 0) {
                            binding.noCommentsLayout.visibility = View.VISIBLE
                        } else {
                            binding.noCommentsLayout.visibility = View.GONE
                        }
                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        commentsAdapter?.retry()

                    }
                    is LoadState.Loading -> {
                        // The adapter has started to load an additional page
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        if (loadStates.append.endOfPaginationReached) {
                            // The adapter has finished loading all of the data set
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    //retrieve database comments count
    private fun getCommentsCount() {
        if (postId != null) {
            dbRef.child("posts").child(postId).child("commentsCount")
                .get().addOnSuccessListener {
                        snapShot ->
                    if(snapShot.getValue(Int::class.java) != null) {
                        commentsCount = snapShot.getValue(Int::class.java)!!
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        //listener for contributions count used to set count text
        if (postId != null) {
            dbRef.child("posts").child(postId)
                .child("commentsCount")
                .addValueEventListener(commentsCountValueEventListener)
        }
        //register observer to adapter to scroll to  position when new items are added
        commentsAdapter?.registerAdapterDataObserver(commentsAdapterObserver)
    }

    override fun onStop() {
        super.onStop()
        //clear observers and listeners
        if (postId != null) {
            dbRef.child("posts").child(postId).child("commentsCount")
                .removeEventListener(commentsCountValueEventListener)
        }
        //unregister adapter observer
        commentsAdapter?.unregisterAdapterDataObserver(commentsAdapterObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(comment: Comment, view: View) {

    }

    //To delete a comment from a post
    override fun onItemLongCLicked(comment: Comment, view: View) {
        if (comment.commenterId == uid) {
            AlertDialog.Builder(requireContext())
                .setMessage("Delete comment?")
                .setPositiveButton("Yes") { dialog, which ->
                    //locate and delete comment from database only if the user is the owner of the
                    // comment
                    comment.commentId?.let { commentId ->
                        if (postId != null) {
                            dbRef.child("post-comments").child(postId)
                                .child(commentId).get().addOnSuccessListener {
                                    dataSnapshot ->
                                    //get comment first, if it is not null, delete and update count
                                    //so as to avoid updating count while comment no longer exits
                                    //when delete us selected multiple times
                                    val commentSnap = dataSnapshot.getValue(Comment::class.java)
                                    if (commentSnap != null) {
                                        dbRef.child("post-comments").child(postId)
                                            .child(commentId).setValue(null) { error, ref ->
                                                //if comment was successfully deleted, decrease comments count
                                                if (error == null) {
                                                    dbRef.child("posts").child(postId)
                                                        .child("commentsCount").runTransaction(object :
                                                            Transaction.Handler {
                                                            override fun doTransaction(currentData: MutableData):
                                                                    Transaction.Result {
                                                                var count =
                                                                    currentData.getValue(Int::class.java)
                                                                if (count != null) {
                                                                    count--
                                                                    currentData.value = count
                                                                }
                                                                return Transaction.success(currentData)
                                                            }

                                                            override fun onComplete(
                                                                error: DatabaseError?,
                                                                committed: Boolean,
                                                                currentData: DataSnapshot?
                                                            ) {
                                                                if (error == null) {
                                                                    Toast.makeText(
                                                                        requireContext(),
                                                                        "Deleted, Refresh",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }

                                                        })
                                                }
                                            }
                                    }
                                }

                        }
                    }
                    dialog.dismiss()
                }.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }.show()
        }
    }

    override fun onUserClicked(userId: String, view: View) {

    }


}