package com.colley.android.view

import android.content.DialogInterface
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var header: View
    private lateinit var photoEventListener: ValueEventListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //set action bar to use custom toolbar
        setSupportActionBar(view.findViewById(R.id.toolbar))

        //find the nav controller associated with the navHost contained within this activity
        navController = findNavController(R.id.mainNavGraphFragmentContainerView)
        drawerLayout = binding.mainActivityDrawerLayout

        //connect the DrawerLayout to your navigation graph by passing it to AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)

        //connect action bar to navController and provide app bar configuration
        //(this controls how the navigation button is displayed: up button or drawer button)
        setupActionBarWithNavController(navController, appBarConfiguration)

        //sets up navigation view for use with navController
        binding.mainActivityNavigationView.setupWithNavController(navController)

        //init Realtime Database
        dbRef = Firebase.database.reference

        //init Firebase Auth
        auth = Firebase.auth

        //initialize a reference to navigation view header
        header = binding.mainActivityNavigationView.getHeaderView(0)

        setUpUserHome()

    }

    private fun setUpUserHome() {

        //sets the profile photo, name and email in the header of the navigation view within the drawerLayout
        header.findViewById<TextView>(R.id.profileNameTextView).text = auth.currentUser?.displayName
        header.findViewById<TextView>(R.id.profileEmailTextView).text = auth.currentUser?.email
        val imageView = header.findViewById<ShapeableImageView>(R.id.profileImageView)

        photoEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()
                if (photo != null) {
                    Glide.with(this@MainActivity).load(photo).into(imageView)
                } else {
                    Log.w(TAG, "photo is null")
                    Snackbar.make(binding.root, "No profile photo set yet", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG,"getPhoto:onCancelled", error.toException())
            }
        }
        dbRef.child("photos").child(auth.currentUser?.uid!!).addListenerForSingleValueEvent(photoEventListener)

    }

    //method called when user tries to navigate up within an activity's hierarchy to a previous screen
    //we override this method so that we pass the navigation task to the navController to take care of appropriately
    override fun onSupportNavigateUp() = navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
            super.onBackPressed()
        }
    }

    fun logOut(item: MenuItem) {
        AlertDialog.Builder(this)
            .setMessage("Confirm logout?")
            .setPositiveButton("Yes") {
                    dialog, which -> AuthUI.getInstance().signOut(this)
            finish()
            }.setNegativeButton("No") {
                    dialog, which -> dialog.dismiss()
            }.show()
    }

    override fun onStop() {
        super.onStop()
        dbRef.child("photos").child(auth.currentUser?.uid!!).removeEventListener(photoEventListener)
    }

    companion object{
        const val TAG = "MainActivity"
    }

}