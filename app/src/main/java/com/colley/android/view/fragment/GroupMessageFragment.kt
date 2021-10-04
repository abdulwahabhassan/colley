package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.GroupMessageRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentGroupMessageBinding
import com.colley.android.model.GroupMessage
import com.colley.android.model.PrivateMessage
import com.colley.android.model.SendButtonObserver
import com.colley.android.observer.GroupMessageScrollToBottomObserver
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class GroupMessageFragment :
    Fragment(),
    GroupMessageRecyclerAdapter.DataChangedListener,
    GroupMessageRecyclerAdapter.ItemClickedListener {

    private val args: GroupMessageFragmentArgs by navArgs()
    private var _binding: FragmentGroupMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var adapter: GroupMessageRecyclerAdapter? = null
    private lateinit var manager: WrapContentLinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            onImageSelected(uri)
        }
    }
    private var actionMode: ActionMode? = null
    private var listOfSelectedMessages = arrayListOf<String>()
    private var selectedMessagesCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //allows this fragment to be able to modify it's containing activity's toolbar menu
        setHasOptionsMenu(true);
    }

    //since we have set hasOptionsMenu to true, our fragment can now override this call to allow us
    //modify the menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        //this inflates a new menu
        inflater.inflate(R.menu.group_message_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.group_info_menu_item -> {
                val action = GroupMessageFragmentDirections.actionGroupMessageFragmentToGroupInfoFragment(args.groupId)
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupMessageBinding.inflate(inflater, container, false)
        recyclerView = binding.messageRecyclerView
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

        //set group name
        dbRef.child("groups").child(args.groupId).child("name").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupName = snapshot.getValue<String>()
                    //set action bar title
                    if (groupName != null) {
                        (activity as AppCompatActivity?)?.supportActionBar?.title = groupName
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "getGroupName:OnCancelled", error.toException())
                }
            }
        )


        //get a query reference to group messages
        val messagesRef = dbRef.child("group-messages").child(args.groupId)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<GroupMessage>()
            .setQuery(messagesRef, GroupMessage::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = GroupMessageRecyclerAdapter(options, currentUser, this, this, requireContext())
        manager =  WrapContentLinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        manager.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        //scroll down when a new message arrives
        adapter?.registerAdapterDataObserver(
            GroupMessageScrollToBottomObserver(
                binding.messageRecyclerView,
                adapter!!,
                manager)
        )

        //disable the send button when there's no text in the input field
        binding.messageEditText.addTextChangedListener(SendButtonObserver(binding.sendButton))

        //when the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {
            if (binding.messageEditText.text?.trim()?.toString() != "") {

                //create a reference for the message on user's messages location and retrieve its
                //key with which to update other locations that should have a ref to the message
                val key = dbRef.child("user-messages").child(args.groupId).push().key

                if(key != null) {
                    val groupMessage = GroupMessage(
                        userId = currentUser.uid,
                        text = binding.messageEditText.text.toString(),
                        messageId = key
                    )
                    dbRef.child("group-messages").child(args.groupId).child(key)
                        .setValue(groupMessage)
                    //update group's recent message
                    dbRef.child("group-messages").child("recent-message")
                        .child(args.groupId).setValue(groupMessage)
                    binding.messageEditText.setText("")
                } else {
                    Toast.makeText(requireContext(), "Unsuccessful", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // When the image button is clicked, launch the image picker
        binding.addMessageImageView.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }

    }

    private fun onImageSelected(uri: Uri) {

        val tempMessage = GroupMessage(
            userId = currentUser.uid,
            image = LOADING_IMAGE_URL
        )
        dbRef
            .child("group-messages")
            .child(args.groupId)
            .push()
            .setValue(
                tempMessage,
                DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError != null) {
                        Log.w(
                            TAG, "Unable to write message to database.",
                            databaseError.toException()
                        )
                        return@CompletionListener
                    }
                    // Build a StorageReference and then upload the file
                    val key = databaseReference.key
                    val storageReference = Firebase.storage
                        .getReference(currentUser.uid)
                        .child(key!!)
                        .child(uri.lastPathSegment!!)
                    putImageInStorage(storageReference, uri, key)
                })
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        // First upload the image to Cloud Storage
        storageReference.putFile(uri)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to the message.
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        val groupMessage =
                            GroupMessage(
                                userId = currentUser.uid,
                                image = uri.toString(),
                                messageId = key
                            )
                        dbRef
                            .child("group-messages")
                            .child(args.groupId)
                            .child(key!!)
                            .setValue(groupMessage)
                        //update group's recent message
                        dbRef.child("group-messages").child("recent-message").child(args.groupId).setValue(groupMessage)
                    }
            }
    }


    override fun onStop() {
        super.onStop()
        binding.linearLayout.visibility = VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onDataAvailable() {
        binding.progressBar.visibility = GONE
        binding.linearLayout.visibility = VISIBLE
    }

    companion object {
        private const val TAG = "GroupMessageFragment"
        private const val LOADING_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/colley-c37ea.appspot.com/o/loading_gif%20copy.gif?alt=media&token=022770e5-9db3-426c-9ee2-582b9d66fbac"
    }

    override fun onItemLongCLicked(message: GroupMessage, view: View) {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity?)!!
                .startSupportActionMode(actionModeCallBack)
        }
        //else clause is not used since we want the action mode to be initialized first if null
        //on a first time long click and function should proceed to update selection
        //if we mistakenly add an else clause, an unexpected behaviour will occur, action mode will
        //be initialized the first time on long click, but our selection will not be updated
        if(message.messageId != null) {
            updateSelection(message.messageId!!, view)
        }
    }

    override fun onUserClicked(userId: String, view: View) {
        val action = GroupMessageFragmentDirections.actionGroupMessageFragmentToUserInfoFragment(userId)
        findNavController().navigate(action)
    }

    override fun onItemClicked(message: GroupMessage, root: View) {
        if (actionMode != null) {
            if(message.messageId != null) {
                updateSelection(message.messageId!!, root)
            }
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
                            "Delete " + singularOrPlural(
                                listOfSelectedMessages,
                                "this message",
                                "these messages"
                            ) + "? \nGroup members can only delete their own messages"
                        )
                        .setPositiveButton("Yes") { dialog, which ->
                            //perform operation for all selected messages
                            listOfSelectedMessages.forEach { messageId ->
                                dbRef.child("group-messages").child(args.groupId)
                                    .child(messageId).get()
                                    .addOnSuccessListener { dataSnapshot ->
                                        val message = dataSnapshot.getValue<GroupMessage>()
                                        //if this message was uploaded by the current user, delete
                                        if (message?.userId == currentUser.uid) {
                                            //if this message has an image, delete from database
                                            //storage
                                            if(message.image != null) {
                                                //get a reference to it's location on database storage from its url and
                                                //delete it with the retrieved reference
                                                Firebase.storage.getReferenceFromUrl(message.image!!).delete()
                                                    .addOnCompleteListener { task ->
                                                        if (task.isSuccessful) {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Deleted media",
                                                                Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Failed to delete media",
                                                                Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                            }
                                            //delete message, user can only delete their own messages from the group
                                            dbRef.child("group-messages").child(args.groupId)
                                                .child(messageId).setValue(null)
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
            listOfSelectedMessages.clear()
            //reset count to zero after clearing list
            selectedMessagesCount = listOfSelectedMessages.size
            //reset adapter tracking list to an empty list
            adapter?.resetMessagesSelectedList()
            //reset backgrounds of selected views to white
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
    private fun updateSelection(messageId: String, view: View) {
        if (!listOfSelectedMessages.contains(messageId)) {
            listOfSelectedMessages.add(messageId)
            selectedMessagesCount = listOfSelectedMessages.size
            actionMode?.title = selectedMessagesCount.toString()
        } else {
            listOfSelectedMessages.remove(messageId)
            selectedMessagesCount = listOfSelectedMessages.size
            actionMode?.title = selectedMessagesCount.toString()
        }
        if (selectedMessagesCount == 0) {
            actionMode?.title = null
            actionMode?.finish()
        }
    }


}