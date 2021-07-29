package com.colley.android

import android.os.Bundle
import androidx.appcompat.app.ActionBar
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(view.findViewById(R.id.toolbar))

        //find the nav controller associated with the navHost contained within this activity
        navController = findNavController(R.id.mainNavGraphFragmentContainerView)
        drawerLayout = binding.mainActivityDrawerLayout

        //connect the DrawerLayout to your navigation graph by passing it to AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)

        //To use firebase emulator in development mode
        if (BuildConfig.DEBUG) {
            Firebase.database.useEmulator("10.0.2.2", 9000)
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
        }

        //sets the profile photo in the header of the navigation view within the drawerLayout
        val header = binding.mainActivityNavigationView.getHeaderView(0)
        val imageView = header.findViewById<ShapeableImageView>(R.id.profileImageView)
        Glide.with(this).load(DummyData.getListOfPosts()[2].userPhoto).into(imageView)


        //connect action bar to navController and provide app bar configuration
        //(this controls how the navigation button is displayed: up button or drawer button)
        setupActionBarWithNavController(navController, appBarConfiguration)

        //sets up navigation view for use with navController
        binding.mainActivityNavigationView.setupWithNavController(navController)



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


}