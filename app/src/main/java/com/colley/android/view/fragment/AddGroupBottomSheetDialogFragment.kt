package com.colley.android.view.fragment

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.LogPrinter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.group.AddGroupMembersRecyclerAdapter
import com.colley.android.databinding.FragmentAddGroupBottomSheetDialogBinding
import com.colley.android.model.NewGroup
import com.colley.android.templateModel.DummyData
import com.colley.android.templateModel.GroupMember
import com.colley.android.templateModel.GroupMessage
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

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
    private val uid: String
        get() = currentUser.uid

    interface SaveButtonListener {
        fun onSave()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddGroupBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        recyclerView = binding.addGroupMembersRecyclerView
        val recyclerViewAdapter = AddGroupMembersRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfFriends())
        recyclerView.adapter = recyclerViewAdapter

        with(binding) {
            createGroupButton.setOnClickListener {
                val groupName = binding.addGroupNameEditText.text.toString()
                val groupDescription = binding.addGroupDescriptionEditText.text.toString()

                if( TextUtils.isEmpty(groupName.trim())) {
                    Toast.makeText(requireContext(), "Group name cannot be empty", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                } else {
                    createGroup(groupName, groupDescription)
                }
                saveButtonListener.onSave()
            }
        }

    }

    private fun createGroup(groupName: String, groupDescription: String) {

        //Disable editing during creation
        setEditingEnabled(false)

        //make instance of new group
       val newGroup = NewGroup(
           name = groupName,
           description = groupDescription,
           groupAdmins = arrayListOf(uid))

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
                    Snackbar.make(homeView, "Group created successfully", Snackbar.LENGTH_LONG).show()
            saveButtonListener.onSave()
        })
    }

    //used to disable fields during creation
    private fun setEditingEnabled(enabled: Boolean) {
        with(binding) {
            addGroupNameEditText.isEnabled = enabled
            addGroupDescriptionEditText.isEnabled = enabled
            selectGroupPhotoTextView.isEnabled = enabled
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAG = "AddGroupDialog"
    }

    override fun onItemClick(groupMember: GroupMember) {

    }
}