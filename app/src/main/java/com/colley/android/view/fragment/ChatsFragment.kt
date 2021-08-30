package com.colley.android.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.ChatsRecyclerAdapter
import com.colley.android.databinding.FragmentPrivateChatsBinding
import com.colley.android.model.PrivateChat
import com.colley.android.view.dialog.NewMessageBottomSheetDialogFragment
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class ChatsFragment :
    Fragment(),
    ChatsRecyclerAdapter.BindViewHolderListener,
    ChatsRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentPrivateChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: ChatsRecyclerAdapter? = null
    private var manager: LinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private var newMessageBottomSheetDialog: NewMessageBottomSheetDialogFragment? = null

    private val uid: String
        get() = currentUser.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrivateChatsBinding.inflate(inflater, container, false)
        recyclerView = binding.privateMessagesRecyclerView
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

        //get a query reference to chats
        val chatsRef = dbRef.child("user-messages").child("recent-message").child(uid)

        //add a listener to chatRef to monitor it value on single change. If null, inform user that
        //they have no active chat
        chatsRef.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
                        //build an options to configure adapter. setQuery takes firebase query to listen to and a
                        //model class to which snapShots should be parsed
                        val options = FirebaseRecyclerOptions.Builder<PrivateChat>()
                            .setQuery(chatsRef, PrivateChat::class.java)
                            .build()

                        adapter = ChatsRecyclerAdapter(options, requireContext(), currentUser, this@ChatsFragment, this@ChatsFragment)
                        manager = LinearLayoutManager(requireContext())
                        recyclerView.layoutManager = manager
                        recyclerView.adapter = adapter
                        adapter?.startListening()
                    } else {
                        binding.privateMessagesProgressBar.visibility = GONE
                        binding.noGroupsLayout.visibility = VISIBLE
                        binding.newChatFab.visibility = VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )

        binding.newChatFab.setOnClickListener {
            newMessageBottomSheetDialog = NewMessageBottomSheetDialogFragment()
            newMessageBottomSheetDialog?.show(childFragmentManager, null)
        }

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

    companion object {

    }

    override fun onBind() {
        binding.privateMessagesProgressBar.visibility = GONE
        binding.noGroupsLayout.visibility = GONE
        binding.newChatFab.visibility = VISIBLE
    }

    override fun onItemClick(chateeId: String) {
        val action = ChatsFragmentDirections.actionPrivateMessagesFragmentToPrivateChatFragment(chateeId)
        view?.findNavController()?.navigate(action)
    }
}