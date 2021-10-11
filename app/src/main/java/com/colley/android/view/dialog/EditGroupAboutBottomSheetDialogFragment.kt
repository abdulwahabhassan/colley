package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.colley.android.R
import com.colley.android.databinding.BottomSheetDialogFragmentEditGroupAboutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class EditGroupAboutBottomSheetDialogFragment (
    val parentContext: Context,
    val editGroupAboutListener: EditGroupAboutListener
        ) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDialogFragmentEditGroupAboutBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val groupId: String
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetDialogFragmentEditGroupAboutBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    interface EditGroupAboutListener {
        fun onGroupAboutChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        val bundledDescription = arguments?.getString("aboutKey")
        binding.editAboutEditText.setText(bundledDescription)

        binding.saveAboutButton.setOnClickListener {
            val description = binding.editAboutEditText.text.toString().trim()
            saveAbout(description)
        }
    }

    private fun saveAbout(description: String) {
        setEditingEnabled(false)

        //retrieve group id from bundle arguments and update group description on database
        arguments?.getString("groupIdKey")?.let {
            dbRef.child("groups").child(it).child("description")
                .setValue(description).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(parentContext, "Updated", Toast.LENGTH_SHORT).show()
                    //dismiss dialog
                    editGroupAboutListener.onGroupAboutChanged()
                    this.dismiss()
                } else {
                    Toast.makeText(parentContext, "Unsuccessful", Toast.LENGTH_LONG).show()
                    //re-enable button for interaction
                    setEditingEnabled(true)
                    binding.saveAboutButton.text = getString(R.string.retry_text)
                }
            }
        }
    }

    private fun setEditingEnabled(enabled: Boolean) {
        binding.editAboutEditText.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}