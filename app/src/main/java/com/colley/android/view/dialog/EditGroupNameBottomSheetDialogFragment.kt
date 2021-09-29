package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.colley.android.R
import com.colley.android.databinding.BottomSheetDialogFragmentEditGroupNameBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class EditGroupNameBottomSheetDialogFragment(
    private val parentContext: Context,
    val editGroupNameListener: EditGroupNameListener
) : BottomSheetDialogFragment() {


    private var _binding: BottomSheetDialogFragmentEditGroupNameBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid

    interface EditGroupNameListener {
        fun onGroupNameChanged()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = BottomSheetDialogFragmentEditGroupNameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        val bundledGroupName = arguments?.getString("groupNameKey")
        binding.editGroupNameEditText.setText(bundledGroupName)

        binding.saveGroupNameButton.setOnClickListener {

            val newGroupName = binding.editGroupNameEditText.text.toString().trim()

            //check that group name is less than or equal to 36
            if (newGroupName.length <= 36) {
                verifyIfAdmin(it, newGroupName)
            } else {
                Toast.makeText(parentContext, "Group name is too long", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun verifyIfAdmin(button: View, newGroupName: String) {
        //disable button to prevent multiple clicks
        button.isEnabled = false
        //check if user is an admin, only admins can change group name
        arguments?.getString("groupIdKey")?.let { groupId ->
            dbRef.child("groups").child(groupId).child("groupAdmins").addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val admins = snapshot.getValue<ArrayList<String>>()
                        if (admins != null && admins.contains(uid)) {
                            saveGroupName(newGroupName, button)
                        } else {
                            Toast.makeText(parentContext, "Only admins can change group name", Toast.LENGTH_LONG).show()
                            //re-enable button to allow for interaction
                            button.isEnabled = true
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "getAdmins:OnCancelled", error.toException())
                        button.isEnabled = true
                    }
                }
            )
        }
    }

    private fun saveGroupName(newGroupName: String, button: View) {

        //retrieve group id from bundle arguments and update group name on database
        arguments?.getString("groupIdKey")?.let{ groupId ->

            //update multiple paths in the database at once
                val childUpdates = hashMapOf<String, Any>(
                "/groups/$groupId/name" to newGroupName,
                "/groups-id-name-photo/$groupId/name" to newGroupName
            )
            //update the specified paths defined in the hashMap
            dbRef.updateChildren(childUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(parentContext, "Updated", Toast.LENGTH_LONG).show()
                    editGroupNameListener.onGroupNameChanged()
                    //dismiss dialog
                    this.dismiss()
                } else {
                    Toast.makeText(parentContext, "Unsuccessful", Toast.LENGTH_LONG).show()
                    //re-enable button for interaction
                    button.isEnabled = true
                    binding.saveGroupNameButton.text = getString(R.string.retry_text)
                }
            }
        }
    }

    companion object {
        const val TAG = "EditGroupNameFragment"
    }
}