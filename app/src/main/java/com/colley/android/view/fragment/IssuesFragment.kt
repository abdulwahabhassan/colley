package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat.getColor
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
import com.colley.android.adapter.IssuesPagingAdapter
import com.colley.android.databinding.FragmentIssuesBinding
import com.colley.android.model.Issue
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


class IssuesFragment ():
    Fragment(),
    IssuesPagingAdapter.IssuePagingItemClickedListener {

    private var _binding: FragmentIssuesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: IssuesPagingAdapter? = null
    private var manager: LinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val uid: String
        get() = currentUser.uid
    private var observer = object : RecyclerView.AdapterDataObserver() {
        @SuppressLint("SetTextI18n")
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(positionStart+itemCount)

            Log.d("position", "$positionStart $itemCount ${adapter?.itemCount}")
            //if onItemRangeInserted is not due to initial load
            if(positionStart != 0) {
                when {
                    itemCount > 1 -> {
                        //Toast.makeText(context, "$itemCount new issues, scroll up to see", Toast.LENGTH_SHORT).show()
                        binding.newIssueNotificationTextView.text = "^ $itemCount new issues"
                    }
                    else -> {
                        //Toast.makeText(context, "$itemCount new issue, scroll up to see", Toast.LENGTH_SHORT).show()
                        binding.newIssueNotificationTextView.text = "^ $itemCount new issue"

                    }
                }
                binding.newIssueNotificationTextView.visibility = VISIBLE
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
        inflater.inflate(R.menu.isssues_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_issues_menu_item -> {
                Toast.makeText(context, "Searching issues", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIssuesBinding.inflate(inflater, container, false)
        recyclerView = binding.issueRecyclerView
        swipeRefreshLayout = binding.swipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //get a query reference to issues
        val issuesQuery = dbRef.child("issues")

        //configuration for how the FirebaseRecyclerPagingAdapter should load pages
        val config = PagingConfig(
            pageSize = 5,
            prefetchDistance = 3,
            enablePlaceholders = false
        )

        //Options to configure an FirebasePagingAdapter
        val options = DatabasePagingOptions.Builder<Issue>()
            .setLifecycleOwner(viewLifecycleOwner)
            .setQuery(issuesQuery, config, Issue::class.java)
            .setDiffCallback(object : DiffUtil.ItemCallback<DataSnapshot>() {
                override fun areItemsTheSame(
                    oldItem: DataSnapshot,
                    newItem: DataSnapshot
                ): Boolean {
                    return oldItem.getValue(Issue::class.java)?.issueId == newItem.getValue(Issue::class.java)?.issueId
                }

                override fun areContentsTheSame(
                    oldItem: DataSnapshot,
                    newItem: DataSnapshot
                ): Boolean {
                    return oldItem.getValue(Issue::class.java) == newItem.getValue(Issue::class.java)
                }

            })
            .build()

        //instantiate adapter
        adapter = IssuesPagingAdapter(
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
                        binding.newIssueNotificationTextView.visibility = INVISIBLE
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
                        Toast.makeText(context, "Error fetching issues! Retrying..", Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noIssuesLayout.visibility = VISIBLE
                        adapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        // The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        //remove display no posts available at the moment
                        binding.noIssuesLayout.visibility = GONE
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
        //reversing layout and stacking from end so that the most recent issues appear at the top
        manager?.reverseLayout = true
        manager?.stackFromEnd = true
        recyclerView.layoutManager = manager

        //initialize adapter
        recyclerView.adapter = adapter


        swipeRefreshLayout.setOnRefreshListener {
            adapter?.refresh()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        adapter?.unregisterAdapterDataObserver(observer)
        _binding = null
    }

    //navigate to new fragment with issue id
    override fun onItemClick(issueId: String, view: View) {
        val action = HomeFragmentDirections.actionHomeFragmentToViewIssueFragment(issueId)
        parentFragment?.findNavController()?.navigate(action)

    }

    override fun onItemLongCLicked(issueId: String, view: View) {
    }

    override fun onUserClicked(userId: String, view: View) {
        val action = HomeFragmentDirections.actionHomeFragmentToUserInfoFragment(userId)
        parentFragment?.findNavController()?.navigate(action)
    }

}