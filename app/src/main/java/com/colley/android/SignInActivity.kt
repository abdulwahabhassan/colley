package com.colley.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.colley.android.databinding.ActivitySignInBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

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
    }

    override fun onStart() {
        super.onStart()
        if (Firebase.auth.currentUser == null) {
            // Sign in with FirebaseUI, see docs for more details:
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.ic_sign_in)
                .setAvailableProviders(listOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                )) //these are lists of authentication providers we defined for the app as specified in
                //the firebase project and allow firebase auth UI to build respective UI for each signup screen.
                //Each of these will represent a button
                .setTheme(R.style.Theme_Colley)
                .build()
            signIn.launch(signInIntent)

        } else {
            goToMainActivity()
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            //we show a successful login if signIn is successful
            Toast.makeText(this, "Sign in successful! ${result.idpResponse}", Toast.LENGTH_LONG).show()
            goToMainActivity()
        } else {
            //otherwise, we  inform user that there was an error
            Toast.makeText(
                this,
                "There was an error signing in",
                Toast.LENGTH_LONG).show()

            val response = result.idpResponse
            if (response == null) {
                Log.w(TAG, "Sign in canceled")
            } else {
                Log.w(TAG, "Sign in error", response.error)
            }
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }


    companion object {
        private const val TAG = "SignInActivity"
    }
}