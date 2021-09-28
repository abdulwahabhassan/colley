package com.colley.android.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.databinding.FragmentChateeInfoBinding
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

class ChateeInfoFragment : Fragment() {

    private val args: ChateeInfoFragmentArgs by navArgs()
    private var _binding: FragmentChateeInfoBinding? = null
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
        _binding = FragmentChateeInfoBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue<Profile>()
                if (profile == null) {
                    Log.e(TAG, "profile for user ${args.chateeId} is unexpectedly null")

                } else {
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
        dbRef.child("profiles").child(args.chateeId).addListenerForSingleValueEvent(profileValueEventListener)

        bioValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bio = snapshot.getValue<String>()
                if (bio == null || bio == "") {
                    Log.e(TAG, "bio for user ${args.chateeId} is unexpectedly null")
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
        dbRef.child("bios").child(args.chateeId).addListenerForSingleValueEvent(bioValueEventListener)

        photoValueEventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()
                if (photo == null) {
                    Log.e(TAG, "photo for user ${args.chateeId} is unexpectedly null")
                    binding?.profilePhotoImageView?.let {
                        Glide.with(requireContext()).load(R.drawable.ic_person_light_pearl).into(
                            it
                        )
                    }
                    binding?.photoProgressBar?.visibility = View.GONE
                } else {
                    binding?.profilePhotoImageView?.let {
                        Glide.with(requireContext()).load(photo)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(it)
                    }
                    binding?.photoProgressBar?.visibility = View.GONE
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "getPhoto:onCancelled", error.toException())
                Snackbar.make(requireView(),
                    "Error in fetching photo",
                    Snackbar.LENGTH_LONG).show()
                binding?.photoProgressBar?.visibility = View.GONE
            }
        }

        //add event listener to chatee photo
        dbRef.child("photos").child(args.chateeId).addListenerForSingleValueEvent(photoValueEventListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {
        private const val TAG = "ProfileFragment"
    }

}