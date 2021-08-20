package com.colley.android.view.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.adapter.group.AddGroupMembersRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentAddGroupBottomSheetDialogBinding
import com.colley.android.model.ChatGroup
import com.colley.android.model.NewGroup
import com.colley.android.model.User
import com.colley.android.templateModel.GroupMessage
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
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

class AddGroupBottomSheetDialogFragment (
    private val saveButtonListener: SaveButtonListener,
    private val homeContext: Context,
    private val homeView: View
        ) : BottomSheetDialogFragment(), AddGroupMembersRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentAddGroupBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var selectedMembersCount = 0
    private val selectedMembersList = arrayListOf<String>()
    private val listOfUsers = arrayListOf<User>()
    private val uid: String
        get() = currentUser.uid
    private var groupImageUri: Uri? = null
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            groupImageUri = uri
            displaySelectedPhoto(groupImageUri!!)
        }
    }

    interface SaveButtonListener {
        fun onSave()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddGroupBottomSheetDialogBinding.inflate(inflater, container, false)
        recyclerView = binding.addGroupMembersRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        //automatically add user to the list of members by default
        selectedMembersList.add(uid)

        //add listener to retrieve users and pass them to AddGroupMembersRecyclerAdapter as a list
        dbRef.child("users").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                       listOfUsers.add(it.getValue<User>()!!)
                        val adapter = AddGroupMembersRecyclerAdapter(currentUser, this@AddGroupBottomSheetDialogFragment, requireContext(), listOfUsers)
                        adapter.notifyDataSetChanged()
                        recyclerView.adapter = adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "getUsers:OnCancelled", error.toException() )
                }
            }
        )


        with(binding) {
            selectGroupPhotoTextView.setOnClickListener {
                openDocument.launch(arrayOf("image/*"))
            }

            createGroupButton.setOnClickListener {
                val groupName = binding.addGroupNameEditText.text.toString()
                val groupDescription = binding.addGroupDescriptionEditText.text.toString()

                if( TextUtils.isEmpty(groupName.trim())) {
                    Toast.makeText(requireContext(), "Group name cannot be empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                } else {
                    createGroup(groupName, groupDescription, groupImageUri)
                }

            }
        }

    }

    private fun createGroup(groupName: String, groupDescription: String, groupImageUri: Uri?) {

        //Disable editing during creation
        setEditingEnabled(false)

        //make instance of new group
       val newGroup = NewGroup(
           name = groupName,
           description = groupDescription,
           groupAdmins = arrayListOf(uid),
           members = selectedMembersList
       )

        //create and push new group to database, retrieve key and add it as groupId
        dbRef.child("groups").push().setValue(newGroup, DatabaseReference.CompletionListener { error, ref ->
        //in case of error
            if (error != null) {
                Toast.makeText(context, "Unable to create group", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Unable to write message to database.", error.toException())
                setEditingEnabled(true)
                return@CompletionListener
            }
            //after creating group, retrieve its key on the database and set it as its id
                val key = ref.key
                dbRef.child("groups").child(key!!).child("groupId").setValue(key)

            //create a reference to group-messages on database and set initial message to "Welcome to the group"
                dbRef.child("group-messages").child(key).push().setValue(GroupMessage(uid, "I welcome everyone to the group"))

            //if a groupPhoto is selected, retrieve its uri and define a storage path for it
            if (groupImageUri != null) {
                val storageReference = Firebase.storage
                    .getReference(key)
                    .child("$uid-group-photo")

                //upload the photo to storage
                putImageInStorage(storageReference, groupImageUri, key, groupName)
            } else {
                //simply update database without group photo
                    val url = null
                dbRef.child("groups-id-name-photo").child(key).setValue(ChatGroup(key, groupName, url))
            }
                Snackbar.make(homeView, "Group created successfully! Uploading to database..", Snackbar.LENGTH_LONG).show()
                saveButtonListener.onSave()
        })
    }

    //used to disable fields during creation
    private fun setEditingEnabled(enabled: Boolean) {
        with(binding) {
            addGroupNameEditText.isEnabled = enabled
            addGroupDescriptionEditText.isEnabled = enabled
            selectGroupPhotoTextView.isEnabled = enabled
            createGroupButton.isEnabled = enabled
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(user: User) {
        
    }

    //interface method to update selected group members count when a member is selected
    //this method also updates the selected members list that will be sent to the database
    override fun onItemSelected(userId: String, view: CheckBox) {
        if (view.isChecked) {
            selectedMembersCount++
            binding.selectedMemberCountTextView.text = selectedMembersCount.toString()
            if (!selectedMembersList.contains(userId)) {
                selectedMembersList.add(userId)
            }
        } else {
            selectedMembersCount--
            binding.selectedMemberCountTextView.text = selectedMembersCount.toString()
            if (selectedMembersList.contains(userId)) {
                selectedMembersList.remove(userId)
            }
        }
        when (selectedMembersCount) {
            0 -> binding.selectedMemberCountTextView.visibility = GONE
            else -> binding.selectedMemberCountTextView.visibility = VISIBLE
        }
    }

    private fun putImageInStorage(
        storageReference: StorageReference,
        groupImageUri: Uri?,
        key: String,
        groupName: String
    ) {
        // First upload the image to Cloud Storage
        storageReference.putFile(groupImageUri!!)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to database
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        dbRef.child("groups-id-name-photo").child(key).setValue(ChatGroup(key, groupName, uri.toString())).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(homeContext, "Photo uploaded successfully", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(homeContext, "Photo uploaded failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Log.w(TAG, "Image upload task was unsuccessful.", e
                )
            }
    }

    //Display selected photo
    private fun displaySelectedPhoto(groupImageUri: Uri) {
        Glide.with(homeContext).load(groupImageUri).into(binding.addGroupImageView)
    }

    override fun onStop() {
        super.onStop()
    }


    companion object {
        const val TAG = "AddGroupDialog"
    }
}