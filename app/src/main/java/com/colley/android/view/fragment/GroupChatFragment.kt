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
import com.colley.android.adapter.GroupChatFragmentRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentGroupChatBinding
import com.colley.android.templateModel.GroupMessage
import com.colley.android.templateModel.ScrollToBottomObserver
import com.colley.android.templateModel.SendButtonObserver
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class GroupChatFragment : Fragment(), GroupChatFragmentRecyclerAdapter.BindViewHolderListener {

    val args: GroupChatFragmentArgs by navArgs()
    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseDatabase
    private lateinit var adapter: GroupChatFragmentRecyclerAdapter
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

        // Initialize Realtime Database
        db = Firebase.database
        //get a query reference to messages
        val messagesRef = db.reference.child(MESSAGES_CHILD)

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<GroupMessage>()
            .setQuery(messagesRef, GroupMessage::class.java)
            .build()

        adapter = GroupChatFragmentRecyclerAdapter(options, getCurrentUser(), this)
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
                binding.messageEditText.text.toString(),
                getCurrentUserName(),
                null,
                getCurrentUserPhotoUrl()
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
        val user = getCurrentUser()
        val tempMessage = GroupMessage(null, getCurrentUserName(), LOADING_IMAGE_URL, getCurrentUserPhotoUrl())
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
                        .getReference(user!!.uid)
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
                            GroupMessage(null, getCurrentUserName(), uri.toString(), getCurrentUserPhotoUrl())
                        db.reference
                            .child(MESSAGES_CHILD)
                            .child(key!!)
                            .setValue(groupMessage)
                    }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Log.w(
                    TAG,
                    "Image upload task was unsuccessful.",
                    e
                )
            }
    }

    private fun getCurrentUser(): FirebaseUser? {
        return Firebase.auth.currentUser
    }

    private fun getCurrentUserName(): String? {
        val user = Firebase.auth.currentUser
        return if (user != null) {
            user.displayName
        } else "Anonymous"
    }

    private fun getCurrentUserPhotoUrl(): String? {
        val user = Firebase.auth.currentUser
        return user?.photoUrl?.toString()
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
        const val ANONYMOUS = "anonymous"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }

}