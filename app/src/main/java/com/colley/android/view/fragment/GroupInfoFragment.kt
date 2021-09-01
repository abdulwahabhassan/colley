package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.listener.SaveButtonListener
import com.colley.android.adapter.GroupMembersRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentGroupInfoBinding
import com.colley.android.model.GroupChat
import com.colley.android.model.Profile
import com.colley.android.view.dialog.AddGroupMemberBottomSheetDialogFragment
import com.colley.android.view.dialog.EditGroupAboutBottomSheetDialogFragment
import com.colley.android.view.dialog.EditGroupNameBottomSheetDialogFragment
import com.colley.android.view.dialog.MemberInteractionBottomSheetDialogFragment
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class GroupInfoFragment :
    Fragment(),
    GroupMembersRecyclerAdapter.ItemClickedListener {

    private val args: GroupInfoFragmentArgs by navArgs()
    private var _binding: FragmentGroupInfoBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var adapter: GroupMembersRecyclerAdapter
    private lateinit var manager: LinearLayoutManager
    private lateinit var infoValueEventListener: ValueEventListener
    private lateinit var aboutValueEventListener: ValueEventListener
    private lateinit var photoValueEventListener: ValueEventListener
    private var editGroupAboutBottomSheetDialog: EditGroupAboutBottomSheetDialogFragment? = null
    private var addGroupMemberSheetDialog: AddGroupMemberBottomSheetDialogFragment? = null
    private var memberInteractionSheetDialog: MemberInteractionBottomSheetDialogFragment? = null
    private var editGroupNameBottomSheetDialog: EditGroupNameBottomSheetDialogFragment? = null
    val uid: String
        get() = currentUser.uid
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { groupImageUri ->
        if(groupImageUri != null) {
            onImageSelected(groupImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentGroupInfoBinding.inflate(inflater, container, false)
        recyclerView = binding.groupMembersRecyclerView
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

        //event listener for group photo
        infoValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val group = snapshot.getValue<GroupChat>()

                //load group image
                if (group?.groupPhoto != null) {
                    Glide.with(requireContext()).load(group.groupPhoto)
                        .into(binding.groupPhotoImageView).also {
                            binding.photoProgressBar.visibility = GONE
                        }
                } else {
                    Glide.with(requireContext()).load(R.drawable.ic_group)
                        .into(binding.groupPhotoImageView)
                    binding.photoProgressBar.visibility = GONE
                }

                //set group name
                if (group?.name != null) {
                    binding.groupNameTextView.text = group.name
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getGroupPhoto:OnCancelled", error.toException())
            }
        }

        //load group info
        dbRef.child("groups-id-name-photo").child(args.groupId)
            .addValueEventListener(infoValueEventListener)

        //event listener for group description
        aboutValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val description = snapshot.getValue<String>()

                //load group image
                if (description != null) {
                    binding.groupDescriptionTextView.text = description
                } else {
                    binding.groupDescriptionTextView.hint = "Describe this group"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getGroupPhoto:OnCancelled", error.toException())
            }
        }

        //load group description
        dbRef.child("groups").child(args.groupId).child("description")
            .addValueEventListener(aboutValueEventListener)

        //event listener for group photo when updated
        photoValueEventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()

                //load group photo
                if (photo == null) {
                    Log.e(TAG, "group photo is unexpectedly null")
                    Glide.with(requireContext()).load(R.drawable.ic_profile)
                        .into(binding.groupPhotoImageView)
                    binding.photoProgressBar.visibility = GONE
                } else {
                    Glide.with(requireContext()).load(photo).into(binding.groupPhotoImageView)
                    binding.photoProgressBar.visibility = GONE
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getPhoto:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching photo",
                    Snackbar.LENGTH_LONG).show()
                binding.photoProgressBar.visibility = GONE
            }
        }

        //get a query reference to group members
        val messagesRef = dbRef.child("groups").child(args.groupId)
            .child("members")

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<String>()
            .setQuery(messagesRef, String::class.java)
            .build()

        adapter = GroupMembersRecyclerAdapter(
            options,
            currentUser,
            this,
            requireContext(),
            args.groupId)

        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        //open dialog with the current group description
        binding.editAboutTextView.setOnClickListener {
            editGroupAboutBottomSheetDialog = EditGroupAboutBottomSheetDialogFragment()
            editGroupAboutBottomSheetDialog?.arguments = bundleOf(
                "aboutKey" to binding.groupDescriptionTextView.text.toString(),
                "groupIdKey" to args.groupId
            )
            editGroupAboutBottomSheetDialog?.show(childFragmentManager, null)
        }

        //update group photo
        binding.addPhotoButton.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }

        binding.addGroupMemberTextView.setOnClickListener {
        //show dialog to add group member
                addGroupMemberSheetDialog = AddGroupMemberBottomSheetDialogFragment(
                    requireContext(),
                    requireView())

                addGroupMemberSheetDialog?.arguments = bundleOf("groupIdKey" to args.groupId)
                addGroupMemberSheetDialog?.show(childFragmentManager, null)
        }

        //to leave a group
        binding.leaveGroupTextView.setOnClickListener {
            dbRef.child("groups").child(args.groupId).child("groupAdmins")
                .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val admins = snapshot.getValue<ArrayList<String>>()
                        if (admins != null && admins.contains(uid)) {
                            context?.let { context -> Toast.makeText(context,
                                "You cannot leave the group while an admin",
                                Toast.LENGTH_LONG).show()
                            }
                        } else {
                                //check if context is not null to prevent NullPointerException in cases
                                    //where user clicks "leave group" button and back button simultaneously
                            context?.let { context ->
                                //open dialog
                                AlertDialog.Builder(context)
                                    .setMessage("Are you sure you want to leave this group?")
                                    .setPositiveButton("Yes") { dialog, _ ->
                                        //run transaction on database list of group members
                                        dbRef.child("groups").child(args.groupId).child("members").runTransaction(
                                            object : Transaction.Handler {
                                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                                    //retrieve the database list which is a mutable data and store in list else
                                                    //return the same data back to database if null
                                                    val list = currentData.getValue<ArrayList<String>>()
                                                        ?: return Transaction.success(currentData)
                                                    //remove the current user from the list if they exist
                                                    if (list.contains(uid)) {
                                                        list.remove(uid)
                                                    }
                                                    //set the value of the database members to the new list
                                                    currentData.value = list
                                                    //return updated list to database
                                                    return Transaction.success(currentData)
                                                }

                                                override fun onComplete(
                                                    error: DatabaseError?,
                                                    committed: Boolean,
                                                    currentData: DataSnapshot?
                                                ) {
                                                    if (committed && error == null) {
                                                        //run transaction to remove group from user's list of groups they belong to
                                                        dbRef.child("user-groups").child(uid).runTransaction(
                                                            object : Transaction.Handler {
                                                                override fun doTransaction(
                                                                    currentData: MutableData
                                                                ): Transaction.Result {
                                                                    //retrieve the database list, if null, return same null value to database
                                                                    val listOfGroups = currentData.getValue<ArrayList<String>>()
                                                                        ?: return Transaction.success(currentData)
                                                                    //remove group's id in the list of group's this members belongs to
                                                                    if (listOfGroups.contains(args.groupId)) {
                                                                        listOfGroups.remove(args.groupId)
                                                                    }
                                                                    //set database list to this update list and return it
                                                                    currentData.value = listOfGroups
                                                                    return Transaction.success(currentData)
                                                                }

                                                                override fun onComplete(
                                                                    error: DatabaseError?,
                                                                    committed: Boolean,
                                                                    currentData: DataSnapshot?
                                                                ) {
                                                                    if (!committed && error != null) {
                                                                        Log.w(TAG, "updateGroupsList:onComplete:$error")
                                                                    }
                                                                }

                                                            }
                                                        )
                                                        Snackbar.make(requireView(), "You are no longer a member of this group", Snackbar.LENGTH_LONG).show()
                                                    } else {
                                                        Log.d(TAG, "leaveGroupTransaction:onComplete:$error")
                                                    }
                                                }

                                            }
                                        )
                                        dialog.dismiss()
                                    }.setNegativeButton("No") { dialog, _ -> dialog.dismiss()
                                    }.show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
        }

        //edit group name
        binding.editGroupNameButton.setOnClickListener {
            editGroupNameBottomSheetDialog = EditGroupNameBottomSheetDialogFragment(requireContext())
            editGroupNameBottomSheetDialog?.arguments = bundleOf(
                "groupNameKey" to binding.groupNameTextView.text.toString(),
                "groupIdKey" to args.groupId
            )
            editGroupNameBottomSheetDialog?.show(childFragmentManager, null)
        }
    }

    private fun onImageSelected(groupImageUri: Uri) {
        binding.photoProgressBar.visibility = View.VISIBLE
        val storageReference = Firebase.storage
            .getReference(args.groupId)
            .child("${auth.currentUser?.uid!!}-group-photo")
        putImageInStorage(storageReference, groupImageUri)
    }

    private fun putImageInStorage(storageReference: StorageReference, groupImageUri: Uri) {
        // First upload the image to Cloud Storage
        storageReference.putFile(groupImageUri)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to database
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        dbRef.child("groups-id-name-photo").child(args.groupId).child("groupPhoto").setValue(uri.toString())
                            .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Snackbar.make(requireView(), "Photo uploaded successfully.. updating..", Snackbar.LENGTH_LONG).show()
                                //load group photo
                                dbRef.child("groups-id-name-photo").child(args.groupId).child("groupPhoto").addListenerForSingleValueEvent(photoValueEventListener)
                            } else {
                                Snackbar.make(requireView(), "Failed to update profile", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Log.w(TAG, "Image upload task was unsuccessful.", e)
            }
    }

    override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        dbRef.child("groups-id-name-photo").child(args.groupId).child("groupPhoto").removeEventListener(infoValueEventListener)
        dbRef.child("groups").child(args.groupId).child("description").removeEventListener(aboutValueEventListener)
        dbRef.child("groups-id-name-photo").child(args.groupId).child("groupPhoto").removeEventListener(photoValueEventListener)
    }


    //retrieve user profile, open bottom sheet dialog fragment to display user profile
    override fun onItemClick(memberId: String) {
        if (memberId != uid) {
            memberInteractionSheetDialog =  MemberInteractionBottomSheetDialogFragment(requireContext())
            memberInteractionSheetDialog?.arguments = bundleOf("memberIdKey" to memberId)
            memberInteractionSheetDialog?.show(childFragmentManager, null)
        }
    }

    //retrieve user profile and open alert dialog to remove the member from the group
    override fun onItemLongCLicked(memberId: String) {
        if(memberId == uid) {
            Toast.makeText(requireContext(), "Leave the group instead, you cannot remove yourself", Toast.LENGTH_LONG).show()
        } else  {
            dbRef.child("groups").child(args.groupId).child("groupAdmins").addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //retrieve list of group admins
                        val groupAdmins = snapshot.getValue<ArrayList<String>>()
                        //check if current user is an admin. Only admins can remove group members
                        if (groupAdmins?.contains(currentUser.uid) == true) {
                            dbRef.child("profiles").child(memberId).addListenerForSingleValueEvent(
                                object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val profile = snapshot.getValue<Profile>()
                                        if (profile != null) {
                                            //open dialog
                                            AlertDialog.Builder(requireContext())
                                                .setMessage("Remove ${profile.name} from this group?")
                                                .setPositiveButton("Yes") {
                                                        dialog, _ ->
                                                    //run transaction on database list of group members
                                                    dbRef.child("groups").child(args.groupId).child("members").runTransaction(
                                                        object : Transaction.Handler {
                                                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                                                //retrieve the database list which is a mutable data and store in list else
                                                                //return the same data back to database if null
                                                                val list = currentData.getValue<ArrayList<String>>()
                                                                    ?: return Transaction.success(currentData)
                                                                //remove the specified member from the list if they exist
                                                                if (list.contains(memberId)) {
                                                                    list.remove(memberId)
                                                                }
                                                                //set value the value of the database members to the new list
                                                                currentData.value = list
                                                                //return updated list to database
                                                                return Transaction.success(currentData)
                                                            }

                                                            override fun onComplete(
                                                                error: DatabaseError?,
                                                                committed: Boolean,
                                                                currentData: DataSnapshot?
                                                            ) {
                                                                if (committed && error == null) {

                                                                    dbRef.child("user-groups").child(memberId).runTransaction(
                                                                        object : Transaction.Handler {
                                                                            override fun doTransaction(
                                                                                currentData: MutableData
                                                                            ): Transaction.Result {
                                                                                //retrieve the database list, if null, return same null value to database
                                                                                val listOfGroups = currentData.getValue<ArrayList<String>>()
                                                                                    ?: return Transaction.success(currentData)
                                                                                //remove group's id in the list of group's this members belongs to
                                                                                if (listOfGroups.contains(args.groupId)) {
                                                                                    listOfGroups.remove(args.groupId)
                                                                                }
                                                                                //set database list to this update list and return it
                                                                                currentData.value = listOfGroups
                                                                                return Transaction.success(currentData)
                                                                            }

                                                                            override fun onComplete(
                                                                                error: DatabaseError?,
                                                                                committed: Boolean,
                                                                                currentData: DataSnapshot?
                                                                            ) {
                                                                                if (committed && error == null) {
                                                                                   if(groupAdmins.contains(memberId)) {
                                                                                       dbRef.child("groups").child(args.groupId).child("groupAdmins").runTransaction(
                                                                                           object :
                                                                                               Transaction.Handler {
                                                                                               override fun doTransaction(
                                                                                                   currentData: MutableData
                                                                                               ): Transaction.Result {
                                                                                                   //retrieve the database list, if null, return same null value to database
                                                                                                   val listOfAdmins = currentData.getValue<ArrayList<String>>()
                                                                                                       ?: return Transaction.success(currentData)
                                                                                                   //remove member from admin list if they exist
                                                                                                   if (listOfAdmins.contains(memberId)) {
                                                                                                       listOfAdmins.remove(memberId)
                                                                                                   }
                                                                                                   //set database list to this updated list and return it
                                                                                                   currentData.value = listOfAdmins
                                                                                                   return Transaction.success(currentData)
                                                                                               }

                                                                                               override fun onComplete(
                                                                                                   error: DatabaseError?,
                                                                                                   committed: Boolean,
                                                                                                   currentData: DataSnapshot?
                                                                                               ) {}

                                                                                           }
                                                                                       )
                                                                                   }

                                                                                }
                                                                            }

                                                                        }
                                                                    )
                                                                    Snackbar.make(requireView(), "${profile.name} removed successfully", Snackbar.LENGTH_LONG).show()
                                                                } else {
                                                                    Log.d(TAG, "removeMemberTransaction:onComplete:$error")
                                                                }
                                                            }

                                                        }
                                                    )
                                                    dialog.dismiss()
                                                }.setNegativeButton("No") {
                                                        dialog, _ -> dialog.dismiss()
                                                }.show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.w(TAG, "getMemberList:OnCancelled", error.toException())
                                    }
                                }
                            )
                        } else {
                            Toast.makeText(requireContext(), "Only group admins can remove members", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getAdminsList:OnCancelled", error.toException())
                    }
                }
            )
        }

    }

    companion object {
        const val TAG = "groupInfoFragment"
    }


}