package com.colley.android.view.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.colley.android.R
import com.colley.android.listener.SaveButtonListener
import com.colley.android.databinding.FragmentEditBioBottomSheetDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class EditBioBottomSheetDialogFragment: BottomSheetDialogFragment() {

    private var _binding: FragmentEditBioBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditBioBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        val bundledBio = arguments?.getString("bioKey")
        binding.editBioEditText.setText(bundledBio)

        binding.saveBioButton.setOnClickListener {
            val bio = binding.editBioEditText.text.toString().trim()

            if (bio.length <= 350) {
                saveBio(bio)
            } else {
                Toast.makeText(requireContext(), "Bio is too long, keep it short", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun saveBio(bio: String) {
        setEditingEnabled(false)

        dbRef.child("bios").child(uid).setValue(bio).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                parentFragment?.requireView()?.let {
                        view -> Snackbar.make(view, "Bio updated successfully", Snackbar.LENGTH_LONG)
                    .show() }
                //dismiss dialog
                this.dismiss()
            } else {
                parentFragment?.requireView()?.let {
                        view -> Snackbar.make(view, "Failed to update Bio!", Snackbar.LENGTH_LONG)
                    .show() }
                setEditingEnabled(true)
                binding.saveBioButton.text = getString(R.string.retry_text)
            }
        }
    }

    private fun setEditingEnabled(enabled: Boolean) {
        binding.editBioEditText.isEnabled = enabled
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}