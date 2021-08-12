package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.group.GroupMessageRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentGroupChatBinding
import com.colley.android.model.Profile
import com.colley.android.templateModel.GroupMessage
import com.colley.android.templateModel.ScrollToBottomObserver
import com.colley.android.templateModel.SendButtonObserver
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


class GroupChatFragment : Fragment(), GroupMessageRecyclerAdapter.BindViewHolderListener {

    val args: GroupChatFragmentArgs by navArgs()
    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: GroupMessageRecyclerAdapter
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
        inflater.inflate(R.menu.group_chat_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.info_menu_item -> {
                Toast.makeText(context, "Info", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupChatBinding.inflate(inflater, container, false)
        recyclerView = binding.messageRecyclerView
        recyclerView.setHasFixedSize(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //set action bar title
        (activity as AppCompatActivity?)!!.supportActionBar!!.title = args.groupName

        //initialize Realtime Database
        db = Firebase.database

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!

        //get a query reference to messages
        val messagesRef = db.reference.child(MESSAGES_CHILD)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<GroupMessage>()
            .setQuery(messagesRef, GroupMessage::class.java)
            .build()

        adapter = GroupMessageRecyclerAdapter(options, currentUser, this, requireContext())
        manager = LinearLayoutManager(requireContext())
        manager.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        //scroll down when a new message arrives
        adapter.registerAdapterDataObserver(
            ScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )
        //disable the send button when there's no text in the input field
        binding.messageEditText.addTextChangedListener(SendButtonObserver(binding.sendButton))

        //when the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {
            val groupMessage = GroupMessage(
                userId = currentUser.uid,
                text = binding.messageEditText.text.toString()
            )
            db.reference.child("messages").push().setValue(groupMessage)
            binding.messageEditText.setText("")
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
        db.reference
            .child(MESSAGES_CHILD)
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
                                image = uri.toString()
                            )
                        db.reference
                            .child(MESSAGES_CHILD)
                            .child(key!!)
                            .setValue(groupMessage)
                    }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Log.w(TAG, "Image upload task was unsuccessful.", e)
            }
    }


    override fun onResume() {
        super.onResume()
        //hide support action bar
//        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        //undo hiding of support action bar
//        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onBind() {
        binding.progressBar.visibility = GONE
        binding.linearLayout.visibility = VISIBLE
    }

    companion object {
        private const val TAG = "MainActivity"
        const val MESSAGES_CHILD = "messages"
        private const val LOADING_IMAGE_URL = "https://firebasestorage.googleapis.com/v0/b/colley-c37ea.appspot.com/o/loading_gif%20copy.gif?alt=media&token=022770e5-9db3-426c-9ee2-582b9d66fbac"
    }

}