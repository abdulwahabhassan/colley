package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.ChatsRecyclerAdapter
import com.colley.android.databinding.FragmentPrivateChatsBinding
import com.colley.android.model.PrivateChat
import com.colley.android.view.dialog.NewMessageBottomSheetDialogFragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
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
    ChatsRecyclerAdapter.DataChangedListener,
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

    private var actionMode: ActionMode? = null

    private val uid: String
        get() = currentUser.uid

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        //allows this fragment to be able to modify it's containing activity's toolbar menu
//        setHasOptionsMenu(true);
//    }
//
//    //since we have set hasOptionsMenu to true, our fragment can now override this call to allow us
//    //modify the menu
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        menu.clear()
//        //this inflates a new menu
//        inflater.inflate(R.menu.on_long_click_chat_menu, menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.delete_chat_menu_item -> {
//                AlertDialog.Builder(requireContext())
//                    .setMessage("Delete chat with ")
//                    .setPositiveButton("Yes") { dialog, which ->
//
//                        dialog.dismiss()
//                    }.setNegativeButton("No") {
//                            dialog, which -> dialog.dismiss()
//                    }.show()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

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


    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateChat>) {
        binding.privateMessagesProgressBar.visibility = GONE
        binding.newChatFab.visibility = VISIBLE

        if (snapshotArray.isEmpty()) {
            binding.noChatsLayout.visibility = VISIBLE
        } else {
            binding.noChatsLayout.visibility = GONE
        }
    }

    override fun onItemClick(chateeId: String) {
        val action = ChatsFragmentDirections.actionChatsFragmentToPrivateMessageFragment(chateeId)
        findNavController().navigate(action)
    }

    //action mode call back
    private val actionModeCallBack: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.on_long_click_chat_menu, menu)
            mode?.title = "Delete"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.delete_chat_menu_item -> {
                    AlertDialog.Builder(requireContext())
                        .setMessage("Delete chat with ")
                        .setPositiveButton("Yes") { dialog, which ->
                            //dismiss dialog
                            dialog.dismiss()
                            //finish and close action mode by calling onDestroyActionMode method
                            mode?.finish()
                        }.setNegativeButton("No") { dialog, which -> dialog.dismiss()
                            //finish and close action mode by calling onDestroyActionMode method
                            mode?.finish()
                        }.show()

                    true
                } else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
        }

    }

    override fun onItemLongCLicked(chateeId: String) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!.startSupportActionMode(actionModeCallBack)
        }
//        (activity as AppCompatActivity?)!!.supportActionBar?.title = "Yay"
    }

}