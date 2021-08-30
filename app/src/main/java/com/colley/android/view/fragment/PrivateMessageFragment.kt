package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.PrivateMessageRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentPrivateMessageBinding
import com.colley.android.model.PrivateChat
import com.colley.android.model.Profile
import com.colley.android.model.SendButtonObserver
import com.colley.android.observer.PrivateMessageScrollToBottomObserver
import com.firebase.ui.database.FirebaseRecyclerOptions
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
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class PrivateMessageFragment :
    Fragment(),
    PrivateMessageRecyclerAdapter.BindViewHolderListener {

    private val args: PrivateMessageFragmentArgs by navArgs()
    private var _binding: FragmentPrivateMessageBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: PrivateMessageRecyclerAdapter
    private lateinit var manager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            onImageSelected(uri)
        }
    }

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
                val action = PrivateMessageFragmentDirections.actionPrivateMessageFragmentToChateeInfoFragment(args.chateeId)
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
        dbRef.child("profiles").child(args.chateeId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue<Profile>()
                    if (profile != null) {
                        (activity as AppCompatActivity?)!!.supportActionBar!!.title = profile.name
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )

        //get a query reference to messages
        val messagesRef = dbRef.child("user-messages").child(currentUser?.uid).child(args.chateeId)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<PrivateChat>()
            .setQuery(messagesRef, PrivateChat::class.java)
            .build()

        adapter = PrivateMessageRecyclerAdapter(options, currentUser, this, requireContext())
        manager = LinearLayoutManager(requireContext())
        manager.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        //scroll down when a new message arrives
        adapter.registerAdapterDataObserver(
            PrivateMessageScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )
        //disable the send button when there's no text in the input field
        binding.messageEditText.addTextChangedListener(SendButtonObserver(binding.sendButton))

        //when the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {

            if (binding.messageEditText.text?.trim()?.toString() != "") {
                val privateMessage = PrivateChat(
                    fromUserId = currentUser.uid,
                    toUserId = args.chateeId,
                    text = binding.messageEditText.text.toString()
                )
                //create a reference for the message on user's messages location and retrieve its
                //key with which to update other locations that should have a ref to the message
                val key = dbRef.child("user-messages").child(currentUser.uid).child(args.chateeId).push().key

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
                Toast.makeText(requireContext(), "Empty message not sent", Toast.LENGTH_SHORT).show()
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
        val tempMessage = PrivateChat(
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
                        val privateMessage =
                            PrivateChat(
                                fromUserId = currentUser.uid,
                                toUserId = args.chateeId,
                                image = uri.toString()
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
            .addOnFailureListener(requireActivity()) { e ->
                Log.w( TAG, "Image upload task was unsuccessful.", e)
            }
    }

    override fun onBind() {
        binding.progressBar.visibility = View.GONE
        binding.linearLayout.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        binding.linearLayout.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.stopListening()
        _binding = null
    }

    companion object {
        private const val TAG = "PrivateMessageFragment"
        private const val LOADING_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/colley-c37ea.appspot.com/o/loading_gif%20copy.gif?alt=media&token=022770e5-9db3-426c-9ee2-582b9d66fbac"
    }
}