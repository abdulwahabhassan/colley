package com.colley.android.view.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.colley.android.R
import com.colley.android.SaveButtonListener
import com.colley.android.databinding.FragmentEditProfileBottomSheetDialogBinding
import com.colley.android.model.Profile
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class EditProfileBottomSheetDialogFragment(
    private var saveButtonListener: SaveButtonListener
    )
    : BottomSheetDialogFragment() {

    private var _binding: FragmentEditProfileBottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUser: FirebaseUser
    private val uid: String
        get() = currentUser.uid


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditProfileBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRef = Firebase.database.reference
        currentUser = Firebase.auth.currentUser!!

        val bundledName = arguments?.getString("nameKey")
        val bundledSchool = arguments?.getString("schoolKey")
        val bundledCourse = arguments?.getString("courseKey")
        val bundledStatus = arguments?.getString("statusKey")

        with(binding) {
            editNameEditText.setText(bundledName)
            editSchoolEditText.setText(bundledSchool)
            editCourseEditText.setText(bundledCourse)
            editStatusEditText.setText(bundledStatus)
        }


        with(binding) {
            saveProfileButton.setOnClickListener {

                val name = editNameEditText.text.toString()
                val school = editSchoolEditText.text.toString()
                val course = editCourseEditText.text.toString()
                val status = editStatusEditText.text.toString()

                //Fields are required to not be empty
                if( TextUtils.isEmpty(name.trim()) ||
                    TextUtils.isEmpty(school.trim()) ||
                    TextUtils.isEmpty(course.trim()) ||
                    TextUtils.isEmpty(status.trim())) {
                    Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_LONG).show()
                } else {
                    val profile = Profile(name, school, course, status)
                    saveProfile(profile)
                }
            }
        }
    }

    private fun saveProfile(profile: Profile) {

        //Disable editing during profile updating
        setEditingEnabled(false)

        dbRef.child("profiles").child(uid).setValue(profile).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                parentFragment?.requireView()?.let {
                        view -> Snackbar.make(view, "Profile updated successfully", Snackbar.LENGTH_LONG)
                    .show() }
                saveButtonListener.onSave()
            } else {
                parentFragment?.requireView()?.let {
                        view -> Snackbar.make(view, "Failed to update Profile!", Snackbar.LENGTH_LONG)
                    .show() }
                setEditingEnabled(true)
                binding.saveProfileButton.text = getString(R.string.retry_text)
            }
        }
    }

    //used to disable fields during profile update
    private fun setEditingEnabled(enabled: Boolean) {
        with(binding) {
            editNameEditText.isEnabled = enabled
            editSchoolEditText.isEnabled = enabled
            editCourseEditText.isEnabled = enabled
            editStatusEditText.isEnabled = enabled
            saveProfileButton.isEnabled = enabled
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val TAG = "EditProfile"
    }

}