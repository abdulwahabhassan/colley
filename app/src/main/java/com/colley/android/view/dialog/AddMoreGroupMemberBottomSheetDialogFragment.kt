package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.AddMoreGroupMembersRecyclerAdapter
import com.colley.android.databinding.FragmentAddGroupMemberBottomSheetDialogBinding
import com.colley.android.model.User
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddMoreGroupMemberBottomSheetDialogFragment(
    private val groupContext: Context
) : BottomSheetDialogFragment(), AddMoreGroupMembersRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentAddGroupMemberBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var adapter: AddMoreGroupMembersRecyclerAdapter? = null
    private var selectedMembersCount = 0
    private val selectedMembersList = arrayListOf<String>()
    private var listOfExistingMembers: ArrayList<String>? = null
    private var bundledGroupId: String? = null
    private val uid: String
        get() = currentUser.uid



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddGroupMemberBottomSheetDialogBinding
            .inflate(inflater, container, false)
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


        //retrieve list of already existing members
        dbRef.child("groups").child(bundledGroupId!!).child("members").get()
            .addOnSuccessListener { dataSnapshot ->
                val membersList = dataSnapshot.getValue<ArrayList<String>>()
                if (membersList == null || membersList.isEmpty()) {
                    //create a new list and add current user
                    listOfExistingMembers = arrayListOf()
                    selectedMembersList.add(uid)
                } else {
                    listOfExistingMembers = membersList
                }

                //get a query reference to group members
                val usersRef =  dbRef.child("users")

                //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
                //build an options to configure adapter. setQuery takes firebase query to listen
                //to and a model class to which snapShots should be parsed
                val options = FirebaseRecyclerOptions.Builder<User>()
                    .setQuery(usersRef, User::class.java)
                    .setLifecycleOwner(viewLifecycleOwner)
                    .build()

                //provide list of already existing members for adapter to be able to tell apart
                    //existing members from non-members
                adapter = AddMoreGroupMembersRecyclerAdapter(
                    options,
                    listOfExistingMembers!!,
                    currentUser,
                    this,
                    groupContext)

                recyclerView.layoutManager = WrapContentLinearLayoutManager(
                    groupContext,
                    LinearLayoutManager.VERTICAL,
                    false)
                recyclerView.adapter = adapter
            }


            binding.addMemberButton.setOnClickListener {
                //run a transaction to update members list on the database
                dbRef.child("groups").child(bundledGroupId!!).child("members")
                    .runTransaction(
                    object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            //retrieve the database list which is a mutable data and store in list
                            //if null, create new list
                            var list = currentData.getValue<ArrayList<String>>()
                            if(list == null) {
                                list = arrayListOf()
                            }
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

                                //update each new member's groups list which tells which groups they
                                    //each belong to
                                selectedMembersList.forEach {
                                    //run a transaction to update each user's list of groups they
                                    //are a member of
                                    dbRef.child("user-groups").child(it).runTransaction(
                                        object : Transaction.Handler {
                                            override fun doTransaction(currentData: MutableData):
                                                    Transaction.Result {
                                                //retrieve the database list
                                                val listOfGroups =
                                                    currentData.getValue<ArrayList<String>>()
                                                //if the database list returns null, set it to an
                                                //array containing the group's id
                                                return if (listOfGroups == null) {
                                                    currentData.value = arrayListOf(bundledGroupId)
                                                    Transaction.success(currentData)
                                                } else {
                                                    //add group's id to the list of group's this
                                                    //members belongs to
                                                    listOfGroups.add(bundledGroupId!!)
                                                    //set database list to this update list and
                                                    //return it
                                                    currentData.value = listOfGroups
                                                    Transaction.success(currentData)
                                                }

                                            }

                                            override fun onComplete(
                                                error: DatabaseError?,
                                                committed: Boolean,
                                                currentData: DataSnapshot?
                                            ) {}

                                        }
                                    )
                                }

                                when (selectedMembersList.size) {
                                    0 -> {
                                        Toast.makeText(
                                            groupContext,
                                            "No new member selected",
                                            Toast.LENGTH_LONG).show()
                                    }
                                    1 -> {
                                        Toast.makeText(
                                            groupContext,
                                            "1 new member added successfully",
                                            Toast.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        Toast.makeText(
                                            groupContext,
                                            "${selectedMembersList.size} " +
                                                    "new members added successfully",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            //dismiss dialog
                            this@AddMoreGroupMemberBottomSheetDialogFragment.dismiss()
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
        _binding = null
    }

}