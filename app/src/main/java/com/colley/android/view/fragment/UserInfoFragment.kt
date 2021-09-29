package com.colley.android.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.colley.android.R
import com.colley.android.glide.GlideImageLoader
import com.colley.android.databinding.FragmentUserInfoBinding
import com.colley.android.model.Profile
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class UserInfoFragment : Fragment() {

    private val args: UserInfoFragmentArgs by navArgs()
    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    val uid: String
        get() = Firebase.auth.currentUser!!.uid
    private lateinit var profileValueEventListener: ValueEventListener
    private lateinit var bioValueEventListener: ValueEventListener
    private lateinit var photoValueEventListener: ValueEventListener

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
        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue<Profile>()
                if (profile == null) {
                    Log.e(TAG, "profile for user ${args.userId} is unexpectedly null")

                } else {
                    (activity as AppCompatActivity?)?.supportActionBar?.title = profile.name
                    binding?.nameTextView?.text = profile.name
                    binding?.schoolNameTextView?.text = profile.school
                    binding?.courseOfStudyTextView?.text = profile.course
                    binding?.statusTitleTextView?.text = profile.role
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getProfile:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching profile",
                    Snackbar.LENGTH_LONG).show()
            }
        }

        //add event listener to chatee profile
        dbRef.child("profiles").child(args.userId).addListenerForSingleValueEvent(profileValueEventListener)

        bioValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bio = snapshot.getValue<String>()
                if (bio == null || bio == "") {
                    Log.e(TAG, "bio for user ${args.userId} is unexpectedly null")
                    binding?.bioTextView?.hint = "Talk about yourself"
                    binding?.bioTextView?.text = bio
                } else {
                    binding?.bioTextView?.text = bio
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getBio:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching bio",
                    Snackbar.LENGTH_LONG).show()
            }
        }

        //add event listener to chatee bio
        dbRef.child("bios").child(args.userId).addListenerForSingleValueEvent(bioValueEventListener)

        photoValueEventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()
                    if (photo == null) {
                        Log.e(TAG, "photo for user ${args.userId} is unexpectedly null")
                        binding?.profilePhotoImageView?.let {
                            Glide.with(requireContext()).load(R.drawable.ic_person_light_pearl).into(it)
                        }
                        binding?.photoProgressBar?.visibility = GONE
                    } else {
                        val options = RequestOptions()
                            .error(R.drawable.ic_downloading)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                        binding?.profilePhotoImageView?.visibility = VISIBLE
                        //using custom glide image loader to indicate progress in time
                        GlideImageLoader(binding?.profilePhotoImageView, binding?.photoProgressBar).load(photo, options);
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getPhoto:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching photo",
                    Snackbar.LENGTH_LONG).show()
                binding?.photoProgressBar?.visibility = GONE
            }
        }

        //add event listener to chatee photo
        dbRef.child("photos").child(args.userId).addListenerForSingleValueEvent(photoValueEventListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {
        private const val TAG = "ProfileFragment"
    }

}