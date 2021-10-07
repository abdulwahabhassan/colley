package com.colley.android.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.R
import com.colley.android.adapter.IssuesPagingAdapter
import com.colley.android.databinding.FragmentIssuesBinding
import com.colley.android.repository.DatabaseRepository
import com.colley.android.viewmodel.IssuesViewModel
import com.colley.android.factory.ViewModelFactory
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class IssuesFragment ():
    Fragment(),
    IssuesPagingAdapter.IssuePagingItemClickedListener{

    private var _binding: FragmentIssuesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var issuesAdapter: IssuesPagingAdapter? = null
    private var manager: WrapContentLinearLayoutManager? = null
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
        inflater.inflate(R.menu.isssues_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_issues_menu_item -> {
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

        // get the view model
        val viewModel = ViewModelProvider(this, ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(IssuesViewModel::class.java)

        //get a query reference to issues
        val issuesQuery = dbRef.child("issues").orderByChild("endorsementsCount")

        //initialize adapter
        issuesAdapter = IssuesPagingAdapter(requireContext(), currentUser, this)

        //set recycler view layout manager
        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = issuesAdapter

        swipeRefreshLayout.setOnRefreshListener {
            issuesAdapter?.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchIssues(issuesQuery).collectLatest {
                    pagingData ->
                issuesAdapter?.submitData(pagingData)

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            //Perform some action every time data changes or when there is an error.
            issuesAdapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching issues! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noIssuesLayout.visibility = VISIBLE
                        issuesAdapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        // The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        if (issuesAdapter?.itemCount == 0) {
                            binding.noIssuesLayout.visibility = VISIBLE
                        } else {
                            binding.noIssuesLayout.visibility = GONE
                        }
                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        issuesAdapter?.retry()
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

    override fun onDestroy() {
        super.onDestroy()
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