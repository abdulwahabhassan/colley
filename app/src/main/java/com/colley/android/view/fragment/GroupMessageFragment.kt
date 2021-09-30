package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
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
        recyclerView.itemAnimator = DefaultItemAnimator()
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
                val groupMessage = GroupMessage(
                    userId = currentUser.uid,
                    text = binding.messageEditText.text.toString()
                )
                dbRef.child("group-messages").child(args.groupId).push().setValue(groupMessage)
                //update group's recent message
                dbRef.child("group-messages").child("recent-message").child(args.groupId).setValue(groupMessage)
                binding.messageEditText.setText("")
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
                                image = uri.toString()
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
            .addOnFailureListener(requireActivity()) {}
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

    }

    override fun onUserClicked(userId: String, view: View) {
        val action = GroupMessageFragmentDirections.actionGroupMessageFragmentToUserInfoFragment(userId)
        findNavController().navigate(action)
    }

}