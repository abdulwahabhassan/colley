package com.colley.android.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.colley.android.R
import com.colley.android.databinding.ActivitySignInBinding
import com.colley.android.model.Profile
import com.colley.android.model.User
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    //register a callback onSignInResult for the result that is returned from a launched contract with
    //with a given intent
    //FirebaseAuthUIActivityResultContract().This is a contract to return an activity result
    //from an intent and pass the result to the registered callback
    private val signIn: ActivityResultLauncher<Intent> =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            this.onSignInResult(result) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.continueButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onStart() {
        super.onStart()

        //init Realtime Database
        dbRef = Firebase.database.reference

        //init Firebase Auth
        auth = Firebase.auth

        authenticateUser()
    }

    private fun authenticateUser() {
        if (auth.currentUser == null) {

            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.ic_sign_in)
                .setAvailableProviders(listOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )) //these are lists of authentication providers we defined for the app as specified in
                //the firebase project and allow firebase auth UI to build respective UI for each signup screen.
                //Each of these will represent a button
                .setTheme(R.style.Theme_Colley)
                .build()
            signIn.launch(signInIntent)
        } else {
            with(binding) {
                welcomeTextView.visibility = VISIBLE
                continueButton.visibility = VISIBLE
            }
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            Snackbar.make(
                binding.root,
                "Signed in as ${auth.currentUser?.email}",
                Snackbar.LENGTH_LONG).show()
            addUserToDataBase()
        } else {
            //otherwise, we  inform user that there was an error
            Toast.makeText(
                this,
                "Error signing in",
                Toast.LENGTH_LONG).show()
            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }
    }

    //add user to database only if user doesn't already exist
    private fun addUserToDataBase() {

        dbRef.child("users").child(auth.currentUser?.uid!!).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue<User>()
                    if ( user == null) {
                        dbRef.child("users").child(auth.currentUser?.uid!!)
                            .setValue(User(auth.currentUser?.uid, auth.currentUser?.email))
                        binding.welcomeTextView.text = getString(R.string.welcome_text)
                    } else {
                        binding.welcomeTextView.text = getString(R.string.welcome_back_text)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "getUser:onCancelled", error.toException())
                    Snackbar.make(binding.root,
                        "Error in fetching user data",
                        Snackbar.LENGTH_LONG).show()
                }
            }
        )

        dbRef.child("profiles").child(auth.currentUser?.uid!!).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue<Profile>()
                    if (profile == null) {
                        dbRef.child("profiles").child(auth.currentUser?.uid!!)
                            .setValue(Profile(auth.currentUser?.displayName, "", "", ""))
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "getProfileData:onCancelled", error.toException())
                    Snackbar.make(binding.root,
                        "Error in fetching profile data",
                        Snackbar.LENGTH_LONG).show()
                }
            }
        )
    }


    companion object {
        private const val TAG = "SignInActivity"
    }
}