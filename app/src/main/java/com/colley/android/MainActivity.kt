package com.colley.android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.colley.android.databinding.ActivityMainBinding
import com.colley.android.model.DummyData
import com.firebase.ui.auth.AuthUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var header: View

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
        db = Firebase.database

        //init Firebase Auth
        auth = Firebase.auth

        //initialize a reference to navigation view header
        header = binding.mainActivityNavigationView.getHeaderView(0)

        //check if user is authenticated, if not re-direct to signIn screen
        authenticateUser()

        //test-write to firebase
//        val textRef = db.reference.child("text")
//        textRef.push().setValue("fuvk me")

    }

    override fun onStart() {
        super.onStart()
        //authenticate user everytime this activity is started
        authenticateUser()
    }

    private fun authenticateUser() {
        if (auth.currentUser ==  null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        } else {
            //sets the profile photo, name and email in the header of the navigation view within the drawerLayout
            header.findViewById<TextView>(R.id.profileNameTextView).text = auth.currentUser?.displayName
            header.findViewById<TextView>(R.id.profileEmailTextView).text = auth.currentUser?.email
            val imageView = header.findViewById<ShapeableImageView>(R.id.profileImageView)
            Glide.with(this).load(auth.currentUser?.photoUrl).into(imageView)
        }
    }
    //method called when user tries to navigate up within an activity's hierarchy to a previous screen
    //we override this method so that we pass the navigation task to the navController to take care of appropriately
    override fun onSupportNavigateUp() = navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toggle_theme_menu_item -> {
                //action
                true
            }

            R.id.search_menu_item -> {
                //action
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
            super.onBackPressed()
        }
    }

    fun logOut(item: MenuItem) {
        AuthUI.getInstance().signOut(this)
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }


}