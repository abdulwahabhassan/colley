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
import com.google.firebase.database.ktx.getValue
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
    private var selectedChatsCount = 0
    private lateinit var recyclerView: RecyclerView
    private var listOfSelectedChats = arrayListOf<String>()
    private var listOfSelectedChatViews = arrayListOf<View>()
    private var newMessageBottomSheetDialog: NewMessageBottomSheetDialogFragment? = null

    private var actionMode: ActionMode? = null

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

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<PrivateChat>()
            .setQuery(chatsRef, PrivateChat::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = ChatsRecyclerAdapter(options, requireContext(), currentUser, this@ChatsFragment, this@ChatsFragment)
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter


        binding.newChatFab.setOnClickListener {
            newMessageBottomSheetDialog = NewMessageBottomSheetDialogFragment(requireContext())
            newMessageBottomSheetDialog?.show(childFragmentManager, null)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
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

    override fun onItemLongCLicked(chateeId: String, view: View) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!.startSupportActionMode(actionModeCallBack)
        }
        //else clause is not used since we want the action mode to be initialized first if null
        //on a first time long click and function should proceed to update selection
        //if we mistakenly add an else clause, an unexpected behaviour will occur, action mode will
        //be initialized the first time on long click, but our selection will not be updated
        updateSelection(chateeId, view)
    }

    override fun onItemClick(chateeId: String, view: View) {
        if (actionMode == null) {
            val action = ChatsFragmentDirections.actionChatsFragmentToPrivateMessageFragment(chateeId)
            findNavController().navigate(action)
        } else {
            updateSelection(chateeId, view)
        }
    }

    //action mode call back
    private val actionModeCallBack: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.on_long_click_chat_menu, menu)
            mode?.title = "0"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {

            return when (item?.itemId) {
                R.id.delete_chat_menu_item -> {
                    //if only one chat is selected, retrieve chatee's name from database
                    //display the chatee's name in the dialog message
                    if (selectedChatsCount == 1) {
                        dbRef.child("profiles").child(listOfSelectedChats[0])
                            .child("name").get().addOnSuccessListener { dataSnapshot ->
                                val name = dataSnapshot.getValue<String>()
                                //show alert dialog to confirm deletion
                                AlertDialog.Builder(requireContext())
                                    .setMessage("Delete chat with $name?")
                                    .setPositiveButton("Yes") { dialog, which ->
                                        //create an hashmap of paths to set to null
                                        //only the current user's chat reference will be deleted
                                        val childUpdates = hashMapOf<String, Any?>(
                                            "/user-messages/${currentUser.uid}/${listOfSelectedChats[0]}" to null,
                                            "/user-messages/recent-message/${currentUser.uid}/${listOfSelectedChats[0]}" to null
                                        )
                                        //update the specified paths defined in the hashMap
                                        dbRef.updateChildren(childUpdates)
                                        //dismiss dialog
                                        dialog.dismiss()
                                        //finish and close action mode by calling onDestroyActionMode method
                                        mode?.finish()
                                    }.setNegativeButton("No") { dialog, which ->
                                        //dismiss dialog
                                        dialog.dismiss()
                                    }.show()
                            }
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setMessage("Delete $selectedChatsCount chats?")
                            .setPositiveButton("Yes") { dialog, which ->
                                listOfSelectedChats.forEach {
                                    //create an hashmap of paths to set to null
                                    //only the current user's chat reference will be deleted
                                    val childUpdates = hashMapOf<String, Any?>(
                                        "/user-messages/${currentUser.uid}/$it" to null,
                                        "/user-messages/recent-message/${currentUser.uid}/$it" to null
                                    )
                                    //update the specified paths defined in the hashMap
                                    dbRef.updateChildren(childUpdates)
                                }
                                //dismiss dialog
                                dialog.dismiss()
                                //finish and close action mode by calling onDestroyActionMode method
                                mode?.finish()
                            }.setNegativeButton("No") { dialog, which ->
                                //dismiss dialog
                                dialog.dismiss()
                            }.show()
                    }
                    true
                } else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            listOfSelectedChats.clear()
            selectedChatsCount = listOfSelectedChats.size
            //change background resource of all selected views to reflect their deselection
            listOfSelectedChatViews.forEach {
                it.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
            }
            actionMode = null
        }

    }

    //this method is used to keep track of selected items and update the visual state of views
    //It is also used to configure the action mode title
    private fun updateSelection(chateeId: String, view: View) {
        if (!listOfSelectedChats.contains(chateeId)) {
            listOfSelectedChats.add(chateeId)
            listOfSelectedChatViews.add(view)
            selectedChatsCount = listOfSelectedChats.size
            actionMode?.title = selectedChatsCount.toString()
            view.setBackgroundResource(R.drawable.selected_chat_background)
        } else {
            listOfSelectedChats.remove(chateeId)
            listOfSelectedChatViews.remove(view)
            selectedChatsCount = listOfSelectedChats.size
            actionMode?.title = selectedChatsCount.toString()
            view.setBackgroundResource(R.drawable.ripple_effect_curved_edges_16dp)
        }
        if (selectedChatsCount == 0) {
            actionMode?.title = null
            actionMode?.finish()
        }
    }

}