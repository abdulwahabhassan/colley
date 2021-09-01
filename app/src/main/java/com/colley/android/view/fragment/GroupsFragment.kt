package com.colley.android.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.GroupsRecyclerAdapter
import com.colley.android.databinding.FragmentGroupsBinding
import com.colley.android.model.PrivateChat
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class GroupsFragment :
    Fragment(),
    GroupsRecyclerAdapter.ItemClickedListener,
    GroupsRecyclerAdapter.DataChangedListener {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private var adapter: GroupsRecyclerAdapter? = null
    private var manager: LinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.groups_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_groups_menu_item -> {
                Toast.makeText(context, "Searching groups", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        recyclerView = binding.groupRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.groupRecyclerView

        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize currentUser
        currentUser = Firebase.auth.currentUser!!

        //get a query reference to the ids of all the groups this user belongs to
        val groupsRef = dbRef.child("user-groups").child(uid)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<String>()
            .setQuery(groupsRef, String::class.java)
            .build()

        adapter = GroupsRecyclerAdapter(options, requireContext(), currentUser, this@GroupsFragment, this@GroupsFragment)
        manager = LinearLayoutManager(requireContext())
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

    //open group chat fragment, passing group id as argument
    override fun onItemClick(chatGroupId: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToGroupChatFragment(chatGroupId)
        parentFragment?.findNavController()?.navigate(action)
    }


    //hide progress bar when groups are displayed
    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<String>) {
        binding.groupsProgressBar.visibility = GONE
        if(snapshotArray.isEmpty()) {
            binding.noGroupsLayout.visibility = VISIBLE
        } else {
            binding.noGroupsLayout.visibility = GONE
        }
    }

    companion object {
        const val TAG = "GroupsFragment"
    }

}