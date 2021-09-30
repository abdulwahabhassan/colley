package com.colley.android.view.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.colley.android.R
import com.colley.android.glide.GlideImageLoader
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

class ProfileFragment :
    Fragment(),
    EditProfileBottomSheetDialogFragment.EditProfileListener,
    EditBioBottomSheetDialogFragment.EditBioListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var editProfileBottomSheetDialog: EditProfileBottomSheetDialogFragment? = null
    private var editBioBottomSheetDialog: EditBioBottomSheetDialogFragment? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    val uid: String
        get() = Firebase.auth.currentUser!!.uid
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

        //get and set profile details
        getProfile()

        //get and set bio to textview
        getBio()

        //get and load photo to imageview
        getPhoto()

        //bundle pre-existing profile data
        binding.editInfoTextView.setOnClickListener {

            val bundle = bundleOf(
                "nameKey" to binding.nameTextView.text.toString(),
                "schoolKey" to binding.schoolNameTextView.text.toString(),
                "courseKey" to binding.courseOfStudyTextView.text.toString(),
                "statusKey" to binding.statusTitleTextView.text.toString())

            //init dialog
            editProfileBottomSheetDialog = EditProfileBottomSheetDialogFragment(
                requireContext(),
                this)
            //pass pre-existing profile to dialog fragment through arguments
            editProfileBottomSheetDialog?.arguments = bundle
            //show dialog to edit profile
            editProfileBottomSheetDialog?.show(childFragmentManager, null)
        }

        //show dialog to edit bio
        binding.editBioTextView.setOnClickListener {
            editBioBottomSheetDialog = EditBioBottomSheetDialogFragment(
                requireContext(),
                this)
            editBioBottomSheetDialog?.arguments =
                bundleOf("bioKey" to binding.bioTextView.text.toString())
            editBioBottomSheetDialog?.show(childFragmentManager, null)
        }

        binding.addPhotoFab.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }
    }

    //get and set profile details
    private fun getProfile() {
        dbRef.child("profiles").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val profile = dataSnapshot.getValue<Profile>()
            if (profile != null) {
                with(binding) {
                    nameTextView.text = profile.name
                    schoolNameTextView.text = profile.school
                    courseOfStudyTextView.text = profile.course
                    statusTitleTextView.text = profile.role
                }
            }
        }
    }

    //get and load photo to view
    private fun getPhoto() {
        dbRef.child("photos").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val photo = dataSnapshot.getValue<String>()
            with(binding) {
                if (photo == null) {
                    Glide.with(requireContext()).load(R.drawable.ic_person_light_pearl)
                        .into(profilePhotoImageView)
                    photoProgressBar.visibility = GONE
                } else {
                    val options = RequestOptions()
                        .error(R.drawable.ic_downloading)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                    profilePhotoImageView.visibility = VISIBLE
                    //using custom glide image loader to indicate progress in time
                    GlideImageLoader(profilePhotoImageView, photoProgressBar).load(photo, options);
                }
            }

        }
    }

    //get and set bio
    private fun getBio() {
        dbRef.child("bios").child(uid).get().addOnSuccessListener {
                dataSnapshot ->
            val bio = dataSnapshot.getValue<String>()
            if (bio == null || bio == "") {
                binding.bioTextView.hint = "Talk about yourself"
                binding.bioTextView.text = bio
            } else {
                binding.bioTextView.text = bio
            }
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
                        dbRef.child("photos").child(uid).setValue(uri.toString())
                            .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "Updated",
                                    Toast.LENGTH_LONG).show()
                                //get photo to show display updated photo
                                getPhoto()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Unsuccessful",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    //on edit profile, re-fetch profile
    override fun onEditProfile() {
        getProfile()
    }

    //on edit bio, re-fetch bio
    override fun onEditBio() {
        getBio()
    }

}