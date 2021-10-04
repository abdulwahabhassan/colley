package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.PrivateMessageRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentPrivateMessageBinding
import com.colley.android.model.*
import com.colley.android.observer.PrivateMessageScrollToBottomObserver
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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class PrivateMessageFragment :
    Fragment(),
    PrivateMessageRecyclerAdapter.DataChangedListener,
    PrivateMessageRecyclerAdapter.ItemClickedListener {

    private val args: PrivateMessageFragmentArgs by navArgs()
    private var _binding: FragmentPrivateMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: PrivateMessageRecyclerAdapter
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
        inflater.inflate(R.menu.private_message_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chatee_info_menu_item -> {
                val action = PrivateMessageFragmentDirections
                    .actionPrivateMessageFragmentToChateeInfoFragment(args.chateeId)
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
        _binding = FragmentPrivateMessageBinding.inflate(inflater, container, false)
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

        //set action bar title as chatee name
        dbRef.child("profiles").child(args.chateeId).get().addOnSuccessListener {
            dataSnapshot ->
            val profile = dataSnapshot.getValue<Profile>()
            if (profile != null) {
                (activity as AppCompatActivity?)?.supportActionBar?.title = profile.name
            }
        }


        //get a query reference to messages
        val messagesRef = dbRef.child("user-messages").child(currentUser.uid).child(args.chateeId)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<PrivateMessage>()
            .setQuery(messagesRef, PrivateMessage::class.java)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        adapter = PrivateMessageRecyclerAdapter(
            options,
            currentUser,
            this,
            this,
            requireContext())

        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)

        manager.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        //scroll down when a new message arrives
        adapter.registerAdapterDataObserver(
            PrivateMessageScrollToBottomObserver(
                binding.messageRecyclerView,
                adapter,
                manager)
        )
        //disable the send button when there's no text in the input field
        binding.messageEditText.addTextChangedListener(SendButtonObserver(binding.sendButton))

        //when the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {

            if (binding.messageEditText.text?.trim()?.toString() != "") {

                //create a reference for the message on user's messages location and retrieve its
                //key with which to update other locations that should have a ref to the message
                val key = dbRef.child("user-messages").child(currentUser.uid)
                    .child(args.chateeId).push().key

                if(key != null) {
                    //instantiate the message using the retrieved key as its id
                    val privateMessage = PrivateMessage(
                        fromUserId = currentUser.uid,
                        toUserId = args.chateeId,
                        text = binding.messageEditText.text.toString(),
                        messageId = key
                    )

                    //used to update multiple paths in the database
                    //here we save a copy of the message to both the sender and receiver's path
                    val childUpdates = hashMapOf<String, Any>(
                        "/user-messages/${currentUser.uid}/${args.chateeId}/$key" to privateMessage,
                        "/user-messages/recent-message/${currentUser.uid}/${args.chateeId}" to privateMessage,
                        "/user-messages/${args.chateeId}/${currentUser.uid}/$key" to privateMessage,
                        "/user-messages/recent-message/${args.chateeId}/${currentUser.uid}" to privateMessage
                    )
                    //the value of the recent message is used when displaying a user's private
                    //messages from different colleagues, so we make a reference for this to
                    //make it easier to retrieve from the database

                    //update the specified paths defined in the hashMap
                    dbRef.updateChildren(childUpdates)

                    //set edit text field to empty text
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
        //a template message with which to temporarily create a ref and retrieve its key for
        //further operations
        val tempMessage = PrivateMessage(
            fromUserId = currentUser.uid,
            toUserId = args.chateeId,
            image = LOADING_IMAGE_URL
        )
        dbRef
            .child("user-messages")
            .child(currentUser.uid)
            .child(args.chateeId)
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
                        .child(args.chateeId)
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
                        val privateMessage = PrivateMessage(
                                fromUserId = currentUser.uid,
                                toUserId = args.chateeId,
                                image = uri.toString(),
                                messageId = key
                            )

                        //used to update multiple paths in the database
                        //here we save a copy of the message to both the sender and receiver's path
                        val childUpdates = hashMapOf<String, Any>(
                            "/user-messages/${currentUser.uid}/${args.chateeId}/$key" to privateMessage,
                            "/user-messages/recent-message/${currentUser.uid}/${args.chateeId}" to privateMessage,
                            "/user-messages/${args.chateeId}/${currentUser.uid}/$key" to privateMessage,
                            "/user-messages/recent-message/${args.chateeId}/${currentUser.uid}" to privateMessage
                        )
                        //the value of the recent message is used when displaying a user's private
                        //messages from different colleagues, so we make a reference for this to
                        //make it easier to retrieve from the database

                        //update the specified paths defined in the hashMap
                        dbRef.updateChildren(childUpdates)

                    }
            }
    }

    @SuppressLint("SetTextI18n")
    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<PrivateMessage>) {

        binding.progressBar.visibility = View.GONE
        binding.linearLayout.visibility = View.VISIBLE

        if (snapshotArray.isEmpty()) {
            dbRef.child("profiles").child(args.chateeId).get().addOnSuccessListener {
                    dataSnapshot ->
                val profile = dataSnapshot.getValue<Profile>()
                if (profile != null) {
                    binding.startChattingTextView.text =
                        "Start a conversation with ${profile.name}"
                }
            }
        } else {
            binding.startChattingTextView.visibility = View.GONE
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemLongCLicked(message: PrivateMessage, view: View) {
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

    override fun onItemClicked(message: PrivateMessage, view: View) {
        if (actionMode != null) {
            if(message.messageId != null) {
                updateSelection(message.messageId!!, view)
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
                        .setMessage("Delete ${singularOrPlural(listOfSelectedMessages, "this message", "these messages")}?")
                        .setPositiveButton("Yes") { dialog, which ->
                            //perform operation for all selected messages
                            listOfSelectedMessages.forEach { messageId ->
                                dbRef.child("user-messages").child(currentUser.uid)
                                    .child(args.chateeId).child(messageId).get()
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
                                                            "/user-messages/${currentUser.uid}/${args.chateeId}/" +
                                                                    messageId to null,
                                                            "/user-messages/${args.chateeId}/${currentUser.uid}/" +
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
                                            //the chatee's own reference
                                            //create an hashmap of paths to set to null
                                            //only the current user's message reference will be deleted
                                            val childUpdates = hashMapOf<String, Any?>(
                                                "/user-messages/${currentUser.uid}/${args.chateeId}/" +
                                                        messageId to null
                                            )
                                            //update the specified paths defined in the hashMap
                                            dbRef.updateChildren(childUpdates)
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
            adapter.resetMessagesSelectedList()
            //reset backgrounds of selected views to white
            adapter.restBackgroundOfSelectedViews()
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
            actionMode ?.title = selectedMessagesCount.toString()
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

    companion object {
        private const val TAG = "PrivateMessageFragment"
        private const val LOADING_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/" +
                "colley-c37ea.appspot.com/o/loading_gif%20copy.gif?alt=media&token=022770e5-9db3-" +
                "426c-9ee2-582b9d66fbac"
    }

}