package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.ChatsRecyclerAdapter
import com.colley.android.adapter.IssuesFragmentRecyclerAdapter
import com.colley.android.adapter.IssuesRecyclerAdapter
import com.colley.android.databinding.FragmentIssuesBinding
import com.colley.android.model.DummyData
import com.colley.android.model.Issue
import com.colley.android.model.PrivateChat
import com.colley.android.view.dialog.NewMessageBottomSheetDialogFragment
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class IssuesFragment :
    Fragment(),
    IssuesRecyclerAdapter.ItemClickedListener,
    IssuesRecyclerAdapter.DataChangedListener {

    private var _binding: FragmentIssuesBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: IssuesRecyclerAdapter? = null
    private var manager: LinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private val uid: String
        get() = currentUser.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        //get a query reference to chats //order by endorsementsCount
        //appears on top
        val issuesRef = dbRef.child("issues").orderByChild("endorsementsCount")

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<Issue>()
            .setQuery(issuesRef, Issue::class.java)
            .build()

        adapter = IssuesRecyclerAdapter(options, requireContext(), currentUser, this, this)
        manager = LinearLayoutManager(requireContext())
        //reversing and stacking is actually counterintuitive as used in this scenario, the purpose
        //of the manipulation is such that most recent items appear at the top since firebase does
        //not provide a method to sort queries in descending order
        manager?.reverseLayout = true
        manager?.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        adapter?.startListening()

    }

    override fun onResume() {
        super.onResume()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }


    override fun onDestroy() {
        super.onDestroy()
        adapter?.stopListening()
        _binding = null
    }

    //navigate to new fragment with issue id
    override fun onItemClick(issueId: String, view: View) {
        val action = HomeFragmentDirections.actionHomeFragmentToViewIssueFragment(issueId)
        parentFragment?.findNavController()?.navigate(action)

    }

    override fun onItemLongCLicked(issueId: String, view: View) {

    }

    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<Issue>) {
        binding.issuesProgressBar.visibility = GONE
        if (snapshotArray.isEmpty()) {
            binding.noIssuesLayout.visibility = VISIBLE
        } else {
            binding.noIssuesLayout.visibility = GONE
        }
    }

}