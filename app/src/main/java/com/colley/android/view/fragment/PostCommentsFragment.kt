package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.adapter.CommentsFragmentRecyclerAdapter
import com.colley.android.adapter.IssueCommentsPagingAdapter
import com.colley.android.adapter.PostCommentsPagingAdapter
import com.colley.android.databinding.FragmentCommentsBinding
import com.colley.android.factory.ViewModelFactory
import com.colley.android.model.Comment
import com.colley.android.model.CommentModel
import com.colley.android.model.DummyData
import com.colley.android.repository.DatabaseRepository
import com.colley.android.view.dialog.IssueCommentBottomSheetDialogFragment
import com.colley.android.viewmodel.PostCommentsViewModel
import com.colley.android.viewmodel.ViewIssueViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
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
    private var manager: LinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid
    private val commentsCountValueEventListener = object : ValueEventListener {
        @SuppressLint("SetTextI18n")
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue(Int::class.java)
            if(count != null) {
                differenceCount = count - commentsCount
            }
            if(differenceCount > 0 && count != differenceCount) {
                if(differenceCount == 1) {
                    binding.newCommentNotificationTextView.text = "^ $differenceCount new post"
                } else {
                    binding.newCommentNotificationTextView.text = "^ $differenceCount new posts"
                }
                binding.newCommentNotificationTextView.visibility = View.VISIBLE
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    //observer for adapter item changes
    private val commentsAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(0)
            binding.newCommentNotificationTextView.visibility = View.INVISIBLE
        }
    }

    //listener for recycler view scroll events
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy != 0) {
                binding.newCommentNotificationTextView.visibility = View.INVISIBLE
            }
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

        // get the view model
        val viewModel = ViewModelProvider(this, ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(PostCommentsViewModel::class.java)

        //get a query reference to issue comments ordered by time code so that the most recent
        //comments appear first
        val commentsQuery = postId?.let { dbRef.child("post-comments").child(it).orderByChild("timeId") }

        //initialize adapter
        commentsAdapter = PostCommentsPagingAdapter(requireContext(), currentUser, this)

        //add scroll listener to remove notification text view when recycler view is scrolled
        recyclerView.addOnScrollListener(scrollListener)

        //retrieve number of comments from database to be used for estimating the number of new
        //comments added since the user last refreshed
        getCommentsCount()

        //set recycler view layout manager
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = commentsAdapter

        //refresh adapter everytime refresh action is called on swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            commentsAdapter?.refresh()
            //reset posts count on refresh so that this fragment knows the correct database posts count
            getCommentsCount()

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchPostComments(commentsQuery!!).collectLatest {
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
                .child("commentsCount").addValueEventListener(commentsCountValueEventListener)
        }
        //register observer to adapter to scroll to  position when new items are added
        commentsAdapter?.registerAdapterDataObserver(commentsAdapterObserver)
    }

    override fun onStop() {
        super.onStop()
        //clear observers and listeners
        if (postId != null) {
            dbRef.child("posts").child(postId)
                .child("commentsCount").removeEventListener(commentsCountValueEventListener)
        }
        commentsAdapter?.unregisterAdapterDataObserver(commentsAdapterObserver)
        recyclerView.removeOnScrollListener(scrollListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(comment: Comment, view: View) {

    }

    override fun onItemLongCLicked(comment: Comment, view: View) {

    }

    override fun onUserClicked(userId: String, view: View) {

    }


}