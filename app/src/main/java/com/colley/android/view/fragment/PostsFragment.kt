package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.R
import com.colley.android.adapter.PostPagingViewHolder
import com.colley.android.adapter.PostsPagingAdapter
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.model.Post
import com.colley.android.view.dialog.NewPostBottomSheetDialogFragment
import com.colley.android.view.dialog.PostCommentBottomSheetDialogFragment
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class PostsFragment : Fragment(),
    PostsPagingAdapter.PostPagingItemClickedListener,
    NewPostBottomSheetDialogFragment.NewPostListener,
    PostCommentBottomSheetDialogFragment.CommentListener {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: PostsPagingAdapter? = null
    private var manager: LinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private var postViewHolder: PostPagingViewHolder? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var commentSheetDialog: PostCommentBottomSheetDialogFragment
    private val uid: String
        get() = currentUser.uid

    private var observer = object : RecyclerView.AdapterDataObserver() {
        @SuppressLint("SetTextI18n")
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(positionStart+itemCount)

            Log.d("position", "$positionStart $itemCount")
            //if onItemRangeInserted is not due to initial load
            if(positionStart != 0) {
                when {
                    itemCount > 1 -> {
                        binding.newPostNotificationTextView.text = "^ $itemCount new issues"
                    }
                    else -> {
                        binding.newPostNotificationTextView.text = "^ $itemCount new issue"

                    }
                }
                binding.newPostNotificationTextView.visibility = VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //fragment can participate in populating the options menu
        setHasOptionsMenu(true)

        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.posts_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_posts_menu_item -> {
                Toast.makeText(context, "Searching posts", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        recyclerView = binding.postRecyclerView
        swipeRefreshLayout = binding.swipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postsQuery = dbRef.child("posts")

        //configuration for how the FirebaseRecyclerPagingAdapter should load pages
        val config = PagingConfig(
            pageSize = 5,
            prefetchDistance = 3,
            enablePlaceholders = false
        )

        //Options to configure an FirebaseRecyclerPagingAdapter
        val options = DatabasePagingOptions.Builder<Post>()
            .setLifecycleOwner(viewLifecycleOwner)
            .setQuery(postsQuery, config, Post::class.java)
            .setDiffCallback(object : DiffUtil.ItemCallback<DataSnapshot>() {
                override fun areItemsTheSame(
                    oldItem: DataSnapshot,
                    newItem: DataSnapshot
                ): Boolean {
                    return oldItem.getValue(Post::class.java)?.postId ==
                            newItem.getValue(Post::class.java)?.postId
                }

                override fun areContentsTheSame(
                    oldItem: DataSnapshot,
                    newItem: DataSnapshot
                ): Boolean {
                    return oldItem.getValue(Post::class.java) == newItem.getValue(Post::class.java)
                }

            })
            .build()

        //instantiate adapter
        adapter = PostsPagingAdapter(
            options,
            requireContext(),
            currentUser,
            this)

        adapter?.registerAdapterDataObserver(observer)

        //remove notification text view when recycler view is scrolled
        recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy != 0) {
                        binding.newPostNotificationTextView.visibility = View.INVISIBLE
                    }
                }
            }
        )

        //Perform some action every time data changes or when there is an error.
        viewLifecycleOwner.lifecycleScope.launch {
            adapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching posts! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noPostsLayout.visibility = VISIBLE
                        adapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        //The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        //remove display no posts available at the moment
                        binding.noPostsLayout.visibility = GONE

                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        adapter?.retry()

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

        //set recycler view layout manager
        manager = LinearLayoutManager(requireContext())
        //reversing layout and stacking from end so that the most recent posts appear at the top
        manager?.reverseLayout = true
        manager?.stackFromEnd = true
        recyclerView.layoutManager = manager

        swipeRefreshLayout.setOnRefreshListener {
            adapter?.refresh()
        }

        //initialize adapter
        recyclerView.adapter = adapter
    }

    override fun onItemClick(postId: String, view: View) {

    }

    override fun onItemLongCLicked(postId: String, view: View) {

    }

    override fun onUserClicked(userId: String, view: View) {
        val action = HomeFragmentDirections.actionHomeFragmentToUserInfoFragment(userId)
        parentFragment?.findNavController()?.navigate(action)
    }

    override fun onCommentClicked(postId: String, view: View, viewHolder: PostPagingViewHolder) {
        postViewHolder = viewHolder
        commentSheetDialog = PostCommentBottomSheetDialogFragment(
            requireContext(),
            requireView(),
            this
        )
        commentSheetDialog.arguments = bundleOf("postIdKey" to postId)
        commentSheetDialog.show(parentFragmentManager, null)
    }

    override fun onLikeClicked(postId: String, view: View, viewHolder: PostPagingViewHolder) {

        //Register like on database
        dbRef.child("posts").child(postId).child("likes").child(uid).runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    //retrieve the current value of like at this location
                    val liked = currentData.getValue<Boolean>()
                    currentData.value = liked == null || liked == false
                    //set database liked value to the new update
                    return Transaction.success(currentData)
                }

                //on successful entry, update likes count
                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    //get current value of liked
                    val liked = currentData?.getValue(Boolean::class.java)

                    if (error != null) {
                        Toast.makeText(
                            context,
                            "Unable to write like to database",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //update likes count
                        dbRef.child("posts").child(postId).child("likesCount")
                            .runTransaction(
                                object : Transaction.Handler {
                                    override fun doTransaction(currentData: MutableData):
                                            Transaction.Result {
                                        //retrieve the current value of count at this location
                                        var count = currentData.getValue<Int>()
                                        if (count == null) {
                                            count = 1
                                        }

                                        //if liked, increase count by 1 else decrease by 1
                                        if (liked == true) {
                                            count++
                                        } else {
                                            count--
                                        }
                                            currentData.value = count

                                        //set database count value to the new update
                                        return Transaction.success(currentData)
                                    }

                                    //after successfully updating likes count on database, update ui
                                    @SuppressLint("SetTextI18n")
                                    override fun onComplete(
                                        error: DatabaseError?,
                                        committed: Boolean,
                                        currentData: DataSnapshot?
                                    ) {
                                        when (currentData?.getValue(Int::class.java)) {
                                            0 -> viewHolder.itemBinding.likeCountTextView
                                                .visibility = GONE
                                            1 -> {
                                                viewHolder.itemBinding.likeCountTextView
                                                    .visibility = VISIBLE
                                                viewHolder.itemBinding.likeCountTextView
                                                    .text = "${currentData.getValue(Int::class.java)
                                                        .toString()} like"
                                            }
                                            else -> {
                                                viewHolder.itemBinding.likeCountTextView
                                                    .visibility = VISIBLE
                                                viewHolder.itemBinding.likeCountTextView
                                                    .text = "${currentData?.getValue(Int::class.java)
                                                        .toString()} likes"

                                            }
                                        }
                                        //update likeTextView start drawable depending on the value
                                        //of liked
                                        if (liked != null) {
                                            viewHolder.itemBinding.likeTextView.isActivated = liked
                                        }
                                    }
                                }
                            )
                    }
                }

            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.unregisterAdapterDataObserver(observer)
        _binding = null
    }

    override fun refreshPosts() {
        adapter?.refresh()
    }

    //update view holder ui to display updated comments count
    @SuppressLint("SetTextI18n")
    override fun onComment(currentData: DataSnapshot?) {
        if (postViewHolder != null) {

            when (currentData?.getValue(Int::class.java)) {
                0 -> postViewHolder?.itemBinding?.commentCountTextView?.visibility = GONE
                1 -> {
                    postViewHolder?.itemBinding?.commentCountTextView?.visibility = VISIBLE
                    postViewHolder?.itemBinding?.commentCountTextView?.text =
                        "${currentData?.getValue(Int::class.java).toString()} comment"
                }
                else -> {
                    postViewHolder?.itemBinding?.commentCountTextView?.visibility = VISIBLE
                    postViewHolder?.itemBinding?.commentCountTextView?.text =
                        "${currentData?.getValue(Int::class.java).toString()} comments"
                }
            }
        }
    }
}