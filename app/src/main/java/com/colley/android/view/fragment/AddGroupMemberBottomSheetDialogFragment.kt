package com.colley.android.view.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.GroupInfoFragment
import com.colley.android.adapter.group.AddGroupMembersRecyclerAdapter
import com.colley.android.databinding.FragmentAddGroupMemberBottomSheetDialogBinding
import com.colley.android.model.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddGroupMemberBottomSheetDialogFragment (
    private val saveButtonListener: SaveButtonListener,
    private val groupContext: Context,
    private val homeView: View
        ) : BottomSheetDialogFragment(), AddGroupMembersRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentAddGroupMemberBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var selectedMembersCount = 0
    private val selectedMembersList = arrayListOf<String>()
    private val listOfUsers = arrayListOf<User>()
    private var listOfExistingMembers: ArrayList<String>? = null
    private lateinit var membersValueEventListener: ValueEventListener
    private var bundledGroupId: String? = null
    private val uid: String
        get() = currentUser.uid


    interface SaveButtonListener {
        fun onSave()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddGroupMemberBottomSheetDialogBinding.inflate(inflater, container, false)
        recyclerView = binding.addGroupMembersRecyclerView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        //retrieve passed group id
        bundledGroupId = arguments?.getString("groupIdKey")

        //value event listener for existing members list
        membersValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val membersList = snapshot.getValue<ArrayList<String>>()
                if (membersList != null) {
                    listOfExistingMembers = membersList
                } else {
                    Toast.makeText(groupContext, "No members in this group", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getListOfMembers:OnCancelled", error.toException())
            }
        }

        //if groupId isn't null, retrieve existing group members via a single event listener
        if (bundledGroupId != null) {
            dbRef.child("groups").child(bundledGroupId!!).child("members").addListenerForSingleValueEvent(membersValueEventListener)
        }

        //add listener to retrieve all users and pass them to AddGroupMembersRecyclerAdapter as a list
        dbRef.child("users").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        //filter list of all users for users that do not already exist as members in the group
                        if (listOfExistingMembers?.contains(it.getValue<User>()!!.userId) == false) {
                            listOfUsers.add(it.getValue<User>()!!)
                        }
                        //pass filtered list to recycler adapter
                        val adapter = AddGroupMembersRecyclerAdapter(currentUser, this@AddGroupMemberBottomSheetDialogFragment, requireContext(), listOfUsers)
                        adapter.notifyDataSetChanged()
                        recyclerView.adapter = adapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "getUsers:OnCancelled", error.toException() )
                }
            }
        )


            binding.addMemberButton.setOnClickListener {
                //run a transaction to update members list on the database
                dbRef.child("groups").child(bundledGroupId!!).child("members").runTransaction(
                    object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            //retrieve the database list which is a mutable data and store in list else
                            //return the same data back to database if null
                            val list = currentData.getValue<ArrayList<String>>()
                                ?: return Transaction.success(currentData)
                            //update the list by adding all newly selected members
                            list.addAll(selectedMembersList)
                            //change the value of mutable live data to reflect updated list
                            currentData.value = list
                            //return updated list to database
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(
                            error: DatabaseError?,
                            committed: Boolean,
                            currentData: DataSnapshot?
                        ) {

                            //Notify user if transaction was successful else log error
                            if (committed && error == null) {

                                //update each new member's groups list which tells which groups they each belong to
                                selectedMembersList.forEach {
                                    //run a transaction to update each user's list of groups they are a member of
                                    dbRef.child("user-groups").child(it).runTransaction(
                                        object : Transaction.Handler {
                                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                                //retrieve the database list
                                                val listOfGroups = currentData.getValue<ArrayList<String>>()
                                                //if the database list returns null, set it to an array containing the group's id
                                                if (listOfGroups == null) {
                                                    currentData.value = arrayListOf(bundledGroupId)
                                                    return Transaction.success(currentData)
                                                } else {
                                                    //add group's id to the list of group's this members belongs to
                                                    listOfGroups.add(bundledGroupId!!)
                                                    //set database list to this update list and return it
                                                    currentData.value = listOfGroups
                                                    return Transaction.success(currentData)
                                                }

                                            }

                                            override fun onComplete(
                                                error: DatabaseError?,
                                                committed: Boolean,
                                                currentData: DataSnapshot?
                                            ) {
                                                if (!committed && error != null) {
                                                    Log.d(GroupInfoFragment.TAG, "listOfGroupsTransaction:onComplete:$error")
                                                }
                                            }

                                        }
                                    )
                                }

                                when (selectedMembersList.size) {
                                    0 -> {
                                        Snackbar.make(homeView, "No new member selected", Snackbar.LENGTH_LONG).show()
                                    }
                                    1 -> {
                                        Snackbar.make(homeView, "1 new member added successfully", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        Snackbar.make(homeView, "${selectedMembersList.size} new members added successfully", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Log.d(TAG, "addMemberTransaction:onComplete:$error")
                            }
                            saveButtonListener.onSave()
                        }

                    }
                )
            }

    }


    override fun onItemClick(user: User) {
        
    }

    //interface method to update selected group members count when a member is selected
    //this method also updates the selected members list that will be sent to the database
    override fun onItemSelected(userId: String, view: CheckBox) {
        if (view.isChecked) {
            selectedMembersCount++
            binding.selectedMemberCountTextView.text = selectedMembersCount.toString()
            selectedMembersList.add(userId)
        } else {
            selectedMembersCount--
            binding.selectedMemberCountTextView.text = selectedMembersCount.toString()
            selectedMembersList.remove(userId)
        }
        when (selectedMembersCount) {
            0 -> binding.selectedMemberCountTextView.visibility = GONE
            else -> binding.selectedMemberCountTextView.visibility = VISIBLE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //remove event listener
        bundledGroupId?.let { dbRef.child("groups").child(it).child("members").removeEventListener(membersValueEventListener) }
        _binding = null
    }


    companion object {
        const val TAG = "AddGroupMemberDialog"
    }
}