package com.colley.android.view.activity

import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.databinding.ActivityMainBinding
import com.colley.android.model.Profile
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
    private lateinit var profileEventListener: ValueEventListener


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

        //drawerLayout listener
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}

            //In case where a user updates their profile photo,
            //This allows the change to be instantly reflected on the drawerLayout's navigationView
            //header when the drawerLayout is opened
            override fun onDrawerOpened(drawerView: View) {
                //listens for change in profile photo and updates accordingly
                dbRef.child("photos").child(auth.currentUser?.uid!!)
                    .addListenerForSingleValueEvent(photoEventListener)
                //listens for change in profile name and updates accordingly
                dbRef.child("profiles").child(auth.currentUser?.uid!!)
                    .addListenerForSingleValueEvent(profileEventListener)
            }

        })

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

        val imageView = header.findViewById<ShapeableImageView>(R.id.profileImageView)

        //event listener for profile photo on drawer header
        photoEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photo = snapshot.getValue<String>()
                if (photo != null) {
                    Glide.with(this@MainActivity).load(photo).into(imageView)
                } else {
                    Glide.with(this@MainActivity).load(R.drawable.ic_profile).into(imageView)
                    Log.w(TAG, "photo is null")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG,"getPhoto:onCancelled", error.toException())
            }
        }

        //set email
        header.findViewById<TextView>(R.id.profileEmailTextView).text = auth.currentUser?.email

        //event listener for profile name on drawer header
        profileEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue<Profile>()
                if (profile != null) {
                    header.findViewById<TextView>(R.id.profileNameTextView).text = profile.name
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG,"getProfileName:onCancelled", error.toException())
            }
        }

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


    companion object{
        const val TAG = "MainActivity"
    }

}