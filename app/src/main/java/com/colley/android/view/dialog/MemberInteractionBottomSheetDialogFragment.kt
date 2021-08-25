package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.colley.android.databinding.FragmentAddGroupBottomSheetDialogBinding
import com.colley.android.databinding.FragmentMemberInteractionBottomSheetDialogBinding
import com.colley.android.model.GroupMessage
import com.colley.android.model.PrivateMessage
import com.colley.android.model.Profile
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class MemberInteractionBottomSheetDialogFragment(
    private val parentContext: Context
) : BottomSheetDialogFragment() {

    private var _binding: FragmentMemberInteractionBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMemberInteractionBottomSheetDialogBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //retrieve member id from arguments
        val bundledMemberId = arguments?.getString("memberIdKey")

        //initialize database and current user
        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        if (bundledMemberId != null) {

            //load member name
            dbRef.child("profiles").child(bundledMemberId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val memberProfile = snapshot.getValue<Profile>()
                        if (memberProfile != null) {
                            binding.groupMemberName.text = "${memberProfile.name}, ${memberProfile.school}"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(parentContext, "Failed to load member name", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            //load member photo
            dbRef.child("photos").child(bundledMemberId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val memberPhoto = snapshot.getValue<String>()
                        if (memberPhoto != null) {
                            Glide.with(parentContext).load(memberPhoto).into(binding.groupMemberImageView)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(parentContext, "Failed to load member photo", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }


        //show message text box when user click "send message"
        binding.sendMessageTextView.setOnClickListener {
            binding.editMessageTextInputLayout.visibility = VISIBLE
            binding.sendButton.visibility = VISIBLE
        }

        //send message when clicked
        binding.sendButton.setOnClickListener {

            if (bundledMemberId != null) {
                binding.editMMessageEditText
                if (binding.editMMessageEditText.text?.trim()?.toString() != "") {
                    val privateMessage = PrivateMessage(
                        userId = uid,
                        text = binding.editMMessageEditText.text.toString()
                    )

                    val key = dbRef.child("user-messages").child(uid).child(bundledMemberId).push().key

                    //used to update multiple paths in the database
                    //here we save a copy of the message to both the sender and receiver's path
                    val childUpdates = hashMapOf<String, Any>(
                        "/user-messages/$uid/$bundledMemberId/$key" to privateMessage,
                        "/user-messages/$bundledMemberId/$uid/$key" to privateMessage
                    )
                    dbRef.updateChildren(childUpdates).addOnSuccessListener {
                        Toast.makeText(parentContext, "Message sent", Toast.LENGTH_SHORT).show()
                    }

                    binding.editMMessageEditText.setText("")
                } else {
                    Toast.makeText(parentContext, "Empty message not sent", Toast.LENGTH_SHORT).show()
                }
            }

            //dismiss dialog fragment
            this.dismiss()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAG = "MemberProfile"
    }

}