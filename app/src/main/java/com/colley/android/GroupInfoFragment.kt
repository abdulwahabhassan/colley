package com.colley.android

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.adapter.group.GroupMembersRecyclerAdapter
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentGroupInfoBinding
import com.colley.android.model.ChatGroup
import com.colley.android.view.fragment.EditBioBottomSheetDialogFragment
import com.colley.android.view.fragment.EditGroupAboutBottomSheetDialogFragment
import com.colley.android.view.fragment.EditProfileBottomSheetDialogFragment
import com.colley.android.view.fragment.ProfileFragment
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.snackbar.Snackbar
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


class GroupInfoFragment : Fragment(), GroupMembersRecyclerAdapter.ItemClickedListener, SaveButtonListener {

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
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { groupImageUri ->
        if(groupImageUri != null) {
            onImageSelected(groupImageUri)
        }
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
                val group = snapshot.getValue<ChatGroup>()

                //load group image
                if (group?.groupPhoto != null) {
                    Glide.with(requireContext()).load(group.groupPhoto)
                        .into(binding.groupPhotoImageView).also {
                            binding.photoProgressBar.visibility = GONE
                        }
                } else {
                    Glide.with(requireContext()).load(R.drawable.ic_group).into(binding.groupPhotoImageView)
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
        dbRef.child("groups-id-name-photo").child(args.groupId).addListenerForSingleValueEvent(infoValueEventListener)

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
        dbRef.child("groups").child(args.groupId).child("description").addValueEventListener(aboutValueEventListener)

        //event listener for group photo when updated
        photoValueEventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()

                //load group photo
                if (photo == null) {
                    Log.e(TAG, "group photo is unexpectedly null")
                    Glide.with(requireContext()).load(R.drawable.ic_profile).into(binding.groupPhotoImageView)
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
        val messagesRef = dbRef.child("groups").child(args.groupId).child("members")

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<String>()
            .setQuery(messagesRef, String::class.java)
            .build()

        adapter = GroupMembersRecyclerAdapter(options, currentUser, this, requireContext())
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        //open dialog with the current group description
        binding.editAboutTextView.setOnClickListener {
            editGroupAboutBottomSheetDialog = EditGroupAboutBottomSheetDialogFragment(this)
            editGroupAboutBottomSheetDialog?.arguments = bundleOf(
                "aboutKey" to binding.groupDescriptionTextView.text.toString(),
                "groupIdKey" to args.groupId
            )
            editGroupAboutBottomSheetDialog?.show(childFragmentManager, null)
        }

        //update group photo
        binding.addPhotoFab.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }
    }

    private fun onImageSelected(groupImageUri: Uri) {
        binding.photoProgressBar.visibility = View.VISIBLE
        val storageReference = Firebase.storage
            .getReference(args.groupId)
            .child(groupImageUri.lastPathSegment!!)
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


    override fun onItemClick(memberId: String) {

    }

    companion object {
        const val TAG = "groupInfoFragment"
    }

    override fun onSave() {
        editGroupAboutBottomSheetDialog?.dismiss()
    }
}