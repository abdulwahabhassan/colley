package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
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
import com.colley.android.model.PrivateMessage
import com.colley.android.view.dialog.NewMessageBottomSheetDialogFragment
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


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
    private var manager: WrapContentLinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private var newMessageBottomSheetDialog: NewMessageBottomSheetDialogFragment? = null
    private var actionMode: ActionMode? = null
    private var selectedChatsCount = 0
    private var listOfSelectedChats = arrayListOf<String>()
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
        val chatsRef = dbRef.child("user-messages").child("recent-message")
            .child(uid)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<PrivateChat>()
            .setQuery(chatsRef, PrivateChat::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = ChatsRecyclerAdapter(
            options,
            requireContext(),
            currentUser,
            this@ChatsFragment,
            this@ChatsFragment)

        manager = WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
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
            actionMode = (activity as AppCompatActivity?)!!
                .startSupportActionMode(actionModeCallBack)
        }
        //else clause is not used since we want the action mode to be initialized first if null
        //on a first time long click and function should proceed to update selection
        //if we mistakenly add an else clause, an unexpected behaviour will occur, action mode will
        //be initialized the first time on long click, but our selection will not be updated
        updateSelection(chateeId, view)
    }

    override fun onItemClick(chateeId: String, view: View) {
        if (actionMode == null) {
            val action = ChatsFragmentDirections
                .actionChatsFragmentToPrivateMessageFragment(chateeId)
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
                    AlertDialog.Builder(requireContext())
                        .setMessage(
                            "Delete ${singularOrPlural(listOfSelectedChats, 
                                "this chat", 
                                "these chats")}?")
                        .setPositiveButton("Yes") { dialog, which ->
                            //for each selected chat with a chatId, get a reference to its
                            //location on the user's messages path
                            listOfSelectedChats.forEach { chatId ->
                                //retrieve all the messages in the location(chat) as a list of
                                //dataSnapshots
                                dbRef.child("user-messages").child(currentUser.uid)
                                    .child(chatId)
                                    .get().addOnSuccessListener { dataSnapshot ->
                                        val listOfMessageIds = arrayListOf<String>()
                                        //parse each dataSnapshot as PrivateMessage and for each one
                                        //retrieve its messageId
                                        dataSnapshot.children.forEach { snapshot ->
                                            val messageId = snapshot
                                                .getValue(PrivateMessage::class.java)?.messageId
                                            //add messageId to list
                                            if (messageId != null) {
                                                listOfMessageIds.add(messageId)
                                            }
                                        }
                                        //if list is not empty meaning that there are messages in
                                        //the chats perform the following operation on each message
                                        if (listOfMessageIds.isNotEmpty()) {

                                            //delete reference to recent chat
                                            dbRef.child("/user-messages/recent-message/${currentUser.uid}/${chatId}")
                                                .setValue(null)

                                            listOfMessageIds.forEach { messageId ->
                                                dbRef.child("user-messages").child(currentUser.uid)
                                                    .child(chatId).child(messageId).get()
                                                    .addOnSuccessListener { dataSnapshot ->
                                                        val message = dataSnapshot.getValue<PrivateMessage>()
                                                        //if this message has an image and was uploaded by the
                                                        //current user, delete and remove from storage
                                                        if (message?.image != null && message.fromUserId == currentUser.uid) {
                                                            //get a reference to it's location on database storage from its url and
                                                            //delete it with the retrieved reference
                                                            Firebase.storage.getReferenceFromUrl(message.image!!).delete()
                                                                .addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        //create an hashmap of paths to set to null
                                                                        //delete the current user's message reference and that of the chatee
                                                                        //(we only do this when a message has an image to avoid storage in cases
                                                                        //where both users might delete their references to this message and leave
                                                                        //the image remaining in storage forever)
                                                                        val childUpdates = hashMapOf<String, Any?>(
                                                                            "/user-messages/${currentUser.uid}/${chatId}/" +
                                                                                    messageId to null,
                                                                            "/user-messages/${chatId}/${currentUser.uid}/" +
                                                                                    messageId to null
                                                                        )
                                                                        //update the specified paths defined in the hashMap
                                                                        dbRef.updateChildren(childUpdates)
                                                                        Toast.makeText(
                                                                            requireContext(),
                                                                            "Deleted media and message from both ends",
                                                                            Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(
                                                                            requireContext(),
                                                                            "Failed to delete media, hence message from both ends",
                                                                            Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                        } else {
                                                            //else if message doesn't contain an image or wasn't uploaded by the user
                                                            //simply delete their only their own reference of the message without affecting
                                                            //the chatee's own reference and do not delete media from storage
                                                            //create an hashmap of paths to set to null
                                                            //only the current user's message reference will be deleted
                                                            val childUpdates = hashMapOf<String, Any?>(
                                                                "/user-messages/${currentUser.uid}/${chatId}/" +
                                                                        messageId to null
                                                            )
                                                            //update the specified paths defined in the hashMap
                                                            dbRef.updateChildren(childUpdates)
                                                        }
                                                    }
                                            }
                                            //Toast that chat has been deleted after all messages has bee deleted
                                            Toast.makeText(
                                                requireContext(),
                                                "Deleted chat",
                                                Toast.LENGTH_LONG).show()
                                        } else {
                                            //Toast that chat is empty
                                            Toast.makeText(
                                                requireContext(),
                                                "Empty chat",
                                                Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                            //dismiss dialog
                            dialog.dismiss()
                            //finish and close action mode by calling onDestroyActionMode method
                            mode?.finish()
                        }.setNegativeButton("No") { dialog, which ->
                            //dismiss dialog
                            dialog.dismiss()
                        }.show()
                    true
                } else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            //clear list
            listOfSelectedChats.clear()
            //reset count after clearing list
            selectedChatsCount = listOfSelectedChats.size
            //reset adapter tracking list to an empty list
            adapter?.resetSelectedChatsList()
            //reset backgrounds of selected views
            adapter?.restBackgroundOfSelectedViews()
            actionMode = null
        }
    }


    private fun singularOrPlural(list: ArrayList<String>, singular: String, plural: String): String {
        val size = list.size
        var output = ""
        if (list.isNotEmpty()) {
            output = if (size == 1) {
                singular
            } else {
                plural
            }
        }
        return output
    }

    //this method is used to keep track of selected items and update the visual state of views
    //It is also used to configure the action mode title
    private fun updateSelection(chateeId: String, view: View) {
        if (!listOfSelectedChats.contains(chateeId)) {
            listOfSelectedChats.add(chateeId)
            selectedChatsCount = listOfSelectedChats.size
            actionMode?.title = selectedChatsCount.toString()
        } else {
            listOfSelectedChats.remove(chateeId)
            selectedChatsCount = listOfSelectedChats.size
            actionMode?.title = selectedChatsCount.toString()
        }
        if (selectedChatsCount == 0) {
            actionMode?.title = null
            actionMode?.finish()
        }
    }

}