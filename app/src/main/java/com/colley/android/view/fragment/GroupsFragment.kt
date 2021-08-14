package com.colley.android.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.group.GroupMessageRecyclerAdapter
import com.colley.android.adapter.GroupsRecyclerAdapter
import com.colley.android.databinding.FragmentGroupsBinding
import com.colley.android.templateModel.DummyData
import com.colley.android.model.DatabaseGroup
import com.colley.android.model.ChatGroup
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class GroupsFragment : Fragment(), GroupsRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: GroupsRecyclerAdapter
    private lateinit var manager: LinearLayoutManager
    private val uid: String
        get() = currentUser.uid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        //get a query reference to groups
        val groupsRef = dbRef.child("groups")

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<ChatGroup>()
            .setQuery(groupsRef, ChatGroup::class.java)
            .build()

        adapter = GroupsRecyclerAdapter(options, requireContext(), currentUser, this)
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

    }

    override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_menu_item -> {
                Toast.makeText(context, "Search in groups", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(chatGroup: ChatGroup) {
        val groupId = chatGroup.groupId
        if (groupId != null) {
            val action = HomeFragmentDirections.actionHomeFragmentToGroupChatFragment(groupId)
            parentFragment?.findNavController()?.navigate(action)
        }
    }

}