package com.colley.android.view.dialog

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.view.fragment.GroupInfoFragment
import com.colley.android.adapter.AddGroupMembersRecyclerAdapter
import com.colley.android.adapter.WhoToMessageRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentAddGroupBottomSheetDialogBinding
import com.colley.android.databinding.FragmentNewMessageBottomSheetDialogBinding
import com.colley.android.model.GroupChat
import com.colley.android.model.NewGroup
import com.colley.android.model.User
import com.colley.android.model.GroupMessage
import com.colley.android.view.fragment.ChatsFragmentDirections
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

class NewMessageBottomSheetDialogFragment (
    private val parentContext: Context
        ) :
    BottomSheetDialogFragment(),
    WhoToMessageRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentNewMessageBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var selectedUserId: String? = null
    private val listOfUsers = arrayListOf<User>()
    private val uid: String
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewMessageBottomSheetDialogBinding.inflate(inflater, container, false)
        recyclerView = binding.addGroupMembersRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!


        //add listener to retrieve users and pass them to AddGroupMembersRecyclerAdapter as a list
        dbRef.child("users").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                       listOfUsers.add(it.getValue<User>()!!)
                        val adapter = WhoToMessageRecyclerAdapter(currentUser, this@NewMessageBottomSheetDialogFragment, parentContext, listOfUsers)
                        adapter.notifyDataSetChanged()
                        recyclerView.adapter = adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "getUsers:OnCancelled", error.toException() )
                }
            }
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    //when a user is selected, launch private message fragment and dismiss dialog
    override fun onItemClick(user: User) {
        selectedUserId = user.userId

        selectedUserId?.let {
            ChatsFragmentDirections.actionChatsFragmentToPrivateMessageFragment(it)

        }?.also {
            findNavController().navigate(it)
            this.dismiss()
        }

    }

    companion object {
        const val TAG = "NewMessageDialog"
    }
}