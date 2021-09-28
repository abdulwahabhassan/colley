package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.contract.OpenDocumentContract
import com.colley.android.databinding.FragmentProfileBinding
import com.colley.android.model.Profile
import com.colley.android.view.dialog.EditBioBottomSheetDialogFragment
import com.colley.android.view.dialog.EditProfileBottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var editProfileBottomSheetDialog: EditProfileBottomSheetDialogFragment? = null
    private var editBioBottomSheetDialog: EditBioBottomSheetDialogFragment? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    val uid: String
        get() = Firebase.auth.currentUser!!.uid
    private lateinit var profileValueEventListener: ValueEventListener
    private lateinit var bioValueEventListener: ValueEventListener
    private lateinit var photoValueEventListener: ValueEventListener
    private val openDocument = registerForActivityResult(OpenDocumentContract()) { uri ->
        if(uri != null) {
            onImageSelected(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init Realtime Database
        dbRef = Firebase.database.reference

        //init Firebase Auth
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue<Profile>()
                if (profile == null) {
                    Log.e(TAG, "profile for user $uid is unexpectedly null")

                } else {
                    with(binding) {
                        nameTextView.text = profile.name
                        schoolNameTextView.text = profile.school
                        courseOfStudyTextView.text = profile.course
                        statusTitleTextView.text = profile.role
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getProfile:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching profile",
                    Snackbar.LENGTH_LONG).show()
            }
        }

        //add event listener to user profile in case of update
        dbRef.child("profiles").child(uid).addValueEventListener(profileValueEventListener)

        bioValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bio = snapshot.getValue<String>()
                if (bio == null || bio == "") {
                    Log.e(TAG, "bio for user $uid is unexpectedly null")
                    binding.bioTextView.hint = "Talk about yourself"
                    binding.bioTextView.text = bio
                } else {
                    binding.bioTextView.text = bio
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getBio:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching bio",
                    Snackbar.LENGTH_LONG).show()
            }
        }

        //add event listener to user bio in case of update
        dbRef.child("bios").child(uid).addValueEventListener(bioValueEventListener)

        photoValueEventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()
                if (photo == null) {
                    Log.e(TAG, "photo for user $uid is unexpectedly null")
                    Glide.with(requireContext()).load(R.drawable.ic_person_light_pearl)
                        .into(binding.profilePhotoImageView)
                    binding.photoProgressBar.visibility = GONE
                } else {
                    Glide.with(requireContext()).load(photo)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(binding.profilePhotoImageView)
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

        //add event listener to user photo in case of update
        dbRef.child("photos").child(uid).addValueEventListener(photoValueEventListener)


        //bundle pre-existing profile data
        binding.editInfoTextView.setOnClickListener {

            val bundle = bundleOf(
                "nameKey" to binding.nameTextView.text.toString(),
                "schoolKey" to binding.schoolNameTextView.text.toString(),
                "courseKey" to binding.courseOfStudyTextView.text.toString(),
                "statusKey" to binding.statusTitleTextView.text.toString())

            //init dialog
            editProfileBottomSheetDialog = EditProfileBottomSheetDialogFragment()
            //pass pre-existing profile to dialog fragment through arguments
            editProfileBottomSheetDialog?.arguments = bundle
            //show dialog to edit profile
            editProfileBottomSheetDialog?.show(childFragmentManager, null)
        }

        //show dialog to edit bio
        binding.editBioTextView.setOnClickListener {
            editBioBottomSheetDialog = EditBioBottomSheetDialogFragment()
            editBioBottomSheetDialog?.arguments = bundleOf("bioKey" to binding.bioTextView.text.toString())
            editBioBottomSheetDialog?.show(childFragmentManager, null)
        }

        binding.addPhotoFab.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }


    }

    private fun onImageSelected(uri: Uri) {
        binding.photoProgressBar.visibility = VISIBLE
                    val storageReference = Firebase.storage
                        .getReference(uid)
                        .child("$uid-profile-photo")
                    putImageInStorage(storageReference, uri)
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri) {
        // First upload the image to Cloud Storage
        storageReference.putFile(uri)
            .addOnSuccessListener(
                requireActivity()
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to database
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        dbRef.child("photos").child(uid).setValue(uri.toString()).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Snackbar.make(requireView(), "Photo uploaded successfully.. updating..", Snackbar.LENGTH_LONG).show()
                                dbRef.child("photos").child(uid).addListenerForSingleValueEvent(photoValueEventListener)
                            } else {
                                Snackbar.make(requireView(), "Failed to update profile", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
            }
            .addOnFailureListener(requireActivity()) { e ->
                Log.w(TAG,
                    "Image upload task was unsuccessful.",
                    e
                )
            }
    }

    override fun onDestroy() {
        super.onDestroy()

        //remove event listener
        dbRef.child("profiles").child(uid).removeEventListener(profileValueEventListener)
        dbRef.child("bios").child(uid).removeEventListener(bioValueEventListener)
        dbRef.child("photos").child(uid).removeEventListener(photoValueEventListener)
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }

}