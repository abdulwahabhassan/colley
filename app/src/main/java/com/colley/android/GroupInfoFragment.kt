package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.group.GroupMembersRecyclerAdapter
import com.colley.android.databinding.FragmentGroupInfoBinding
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class GroupInfoFragment : Fragment(), GroupMembersRecyclerAdapter.ItemClickedListener {

    private val args: GroupInfoFragmentArgs by navArgs()
    private var _binding: FragmentGroupInfoBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: GroupMembersRecyclerAdapter
    private lateinit var manager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentGroupInfoBinding.inflate(inflater, container, false)
        recyclerView = binding.groupMembersRecyclerView
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

        //get a query reference to group members
        val messagesRef = dbRef.child("groups").child(args.groupId).child("members")

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<String>()
            .setQuery(messagesRef, String::class.java)
            .build()

        adapter = GroupMembersRecyclerAdapter(options, currentUser, this, requireContext())
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    override fun onItemClick(memberId: String) {

    }

    companion object {

    }
}