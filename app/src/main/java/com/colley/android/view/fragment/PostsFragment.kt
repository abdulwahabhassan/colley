package com.colley.android.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
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
import com.colley.android.adapter.PostsPagingAdapterExp
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.model.Issue
import com.colley.android.model.Post
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class PostsFragment : Fragment(),
    PostsPagingAdapterExp.PostPagingItemClickedListener {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: PostsPagingAdapterExp? = null
    private var manager: LinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val uid: String
        get() = currentUser.uid

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
            pageSize = 30,
            prefetchDistance = 15,
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
                    return oldItem.getValue(Post::class.java)?.postId == newItem.getValue(Post::class.java)?.postId
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
        adapter = PostsPagingAdapterExp(
            options,
            requireContext(),
            currentUser,
            this)

        //Perform some action every time data changes or when there is an error.
        viewLifecycleOwner.lifecycleScope.launch {
            adapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(context, "Error fetching posts! Retrying..", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}