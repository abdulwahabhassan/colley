package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.adapter.PostLikesPagingAdapter
import com.colley.android.databinding.FragmentLikesBinding
import com.colley.android.factory.ViewModelFactory
import com.colley.android.repository.DatabaseRepository
import com.colley.android.viewmodel.PostLikesViewModel
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PostLikesFragment(
    private val postId: String?,
    private val parentContext: Context,
    private val postView: View) : Fragment(), PostLikesPagingAdapter.PostLikeItemClickedListener {

    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PostLikesViewModel
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var likesCount: Int = 0
    private var differenceCount: Int = 0
    private var likesQuery: Query? = null
    private var likesAdapter: PostLikesPagingAdapter? = null
    private var manager: WrapContentLinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid

    //value event listener for likes count
    private val likesCountValueEventListener = object : ValueEventListener {
        @SuppressLint("SetTextI18n")
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue(Int::class.java)
            if(count != null) {
                differenceCount = count - likesCount
            }
            //ignore changes due to adding value event listener to database first time or
            //initial paging load. we know this when current likes count is equal to the difference
            //between the previous count (initial count will be zero) and the current count
            if(differenceCount > 0 && count != differenceCount) {
                if(differenceCount == 1) {
                    binding.newLikeNotificationTextView.text = "^ $differenceCount new like"
                } else {
                    binding.newLikeNotificationTextView.text = "^ $differenceCount new likes"
                }
                binding.newLikeNotificationTextView.visibility = View.VISIBLE
                return

            } else if(differenceCount > 0) {
                //acknowledge changes due to adding listener first time / initial paging load and
                //only when likes count increases, so that if initially, likes count was zero
                //and then increases, we also want to inform the user if they want to refresh,
                //that a new like has occurred after initial page load and adding value event
                //listener to database first time even though the difference between the current
                //like count and the previous likes count equals (suggesting either initial page
                //load or adding value event listener to database first time, which in fact is not
                //the case since both events have already occurred)
                if(differenceCount == 1) {
                    binding.newLikeNotificationTextView.text = "$differenceCount like"
                } else {
                    binding.newLikeNotificationTextView.text = "$differenceCount likes"
                }
                    binding.newLikeNotificationTextView.visibility = View.VISIBLE
                return

            } else {
                //if no change in likes count or likes count decreases, it's not important to show it
                binding.newLikeNotificationTextView.visibility = View.INVISIBLE
            }

        }

        override fun onCancelled(error: DatabaseError) {}
    }

    //observer for adapter item changes due to call on adapter refresh
    private val likesAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(positionStart)
            //remove notification text after scrolling to position
            binding.newLikeNotificationTextView.visibility = View.INVISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        recyclerView = binding.likesRecyclerView
        swipeRefreshLayout = binding.postLikesSwipeRefreshLayout
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
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(PostLikesViewModel::class.java)

        //initialize query reference to post likes whose value equals true
        likesQuery = postId?.let { dbRef.child("post-likes").child(it)}

        //initialize adapter
        likesAdapter = PostLikesPagingAdapter(requireContext(), currentUser, this)

        //retrieve number of comments from database to be used for estimating the number of new
        //comments added since the user last refreshed
        getLikesCount()

        //set recycler view layout manager
        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = likesAdapter

        swipeRefreshLayout.setOnRefreshListener {
            likesAdapter?.refresh()
            //every time we refresh, we get the current count of likes, so that when likes count
            //changes(most importantly increases), the value event listener attached to likes count
            //calculates the difference between current count and the previous count. If the
            //difference is positive (indicating more likes), we display to user that new likes have
            //been added to the post if they want to refresh the current ui state.
            getLikesCount()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            likesQuery?.let {
                viewModel.getPostLikes(it).collectLatest { pagingData ->
                    likesAdapter?.submitData(pagingData)

                }
            }
        }

        //Perform some action every time data changes or when there is an error.
        viewLifecycleOwner.lifecycleScope.launch {
            likesAdapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching comments! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noLikesLayout.visibility = View.VISIBLE
                        likesAdapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        //The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        if (likesAdapter?.itemCount == 0) {
                            binding.noLikesLayout.visibility = View.VISIBLE
                        } else {
                            binding.noLikesLayout.visibility = View.GONE
                        }

                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        likesAdapter?.retry()

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
    private fun getLikesCount() {
        if (postId != null) {
            dbRef.child("posts").child(postId).child("likesCount")
                .get().addOnSuccessListener {
                        snapShot ->
                    if(snapShot.getValue(Int::class.java) != null) {
                        likesCount = snapShot.getValue(Int::class.java)!!
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        //listener for likes count used to set count text
        if (postId != null) {
            dbRef.child("posts").child(postId)
                .child("likesCount").addValueEventListener(likesCountValueEventListener)
        }
        //register observer to adapter to scroll to  position when new items are added
        likesAdapter?.registerAdapterDataObserver(likesAdapterObserver)
    }

    override fun onStop() {
        super.onStop()
        //clear observers and listeners
        if (postId != null) {
            dbRef.child("posts").child(postId).child("likesCount")
                .removeEventListener(likesCountValueEventListener)
        }
        //clear observers and listeners
        likesAdapter?.unregisterAdapterDataObserver(likesAdapterObserver)

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    override fun onItemClick(userId: String, view: View) {

    }

    override fun onUserClicked(userId: String, view: View) {

    }
}