package com.colley.android.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.adapter.NotificationsPagingAdapter
import com.colley.android.databinding.FragmentNotificationsBinding
import com.colley.android.factory.ViewModelFactory
import com.colley.android.repository.DatabaseRepository
import com.colley.android.viewmodel.NotificationsViewModel
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class NotificationsFragment : Fragment(),
    NotificationsPagingAdapter.NotificationPagingItemClickedListener {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var notificationsAdapter: NotificationsPagingAdapter? = null
    private var manager: WrapContentLinearLayoutManager? = null
    lateinit var recyclerView: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        recyclerView = binding.notificationsRecyclerView
        swipeRefreshLayout = binding.swipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // get the view model
        val viewModel = ViewModelProvider(
            this,
            ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(NotificationsViewModel::class.java)

        //get a query reference to notifications
        val notificationsQuery = dbRef.child("user-notifications").child(uid)
            .orderByChild("timeId")

        //initialize adapter
        notificationsAdapter = NotificationsPagingAdapter(
            requireContext(),
            this,
            dbRef
        )

        //set recycler view layout manager
        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = notificationsAdapter

        swipeRefreshLayout.setOnRefreshListener {
            notificationsAdapter?.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fetchNotifications(notificationsQuery).collectLatest {
                    pagingData ->
                notificationsAdapter?.submitData(pagingData)

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            //Perform some action every time data changes or when there is an error.
            notificationsAdapter?.loadStateFlow?.collectLatest { loadStates ->
                when (loadStates.refresh) {
                    is LoadState.Error -> {
                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching notifications! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noNotificationsLayout.visibility = VISIBLE
                        notificationsAdapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        // The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        if (notificationsAdapter?.itemCount == 0) {
                            binding.noNotificationsLayout.visibility = VISIBLE
                        } else {
                            binding.noNotificationsLayout.visibility = GONE
                        }
                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        notificationsAdapter?.retry()
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

    override fun onItemClick(notification: com.colley.android.model.Notification) {
        //Navigate to issue if the notification is for an issue
        if(notification.itemType == "issue") {
            val action = notification.itemId?.let { itemId ->
                NotificationsFragmentDirections.actionNotificationsFragmentToViewIssueFragment(itemId)
            }
            if (action != null) {
                notification.notificationId?.let { notificationId ->
                    dbRef.child("user-notifications").child(uid)
                        .child(notificationId).child("clicked").setValue(true)
                }
                parentFragment?.findNavController()?.navigate(action)
            }
        } else if (notification.itemType == "post") {
            val action = notification.itemId?.let { itemId ->
                NotificationsFragmentDirections.actionNotificationsFragmentToViewPostFragment(itemId)
            }
            if (action != null) {
                notification.notificationId?.let { notificationId ->
                    dbRef.child("user-notifications").child(uid)
                        .child(notificationId).child("clicked").setValue(true)
                }
                parentFragment?.findNavController()?.navigate(action)
            }

        }

    }

}