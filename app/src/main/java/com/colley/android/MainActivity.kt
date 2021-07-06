package com.colley.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBarWithNavController(findNavController(R.id.mainNavGraphFragmentContainerView))
    }
    override fun onSupportNavigateUp() = findNavController(R.id.mainNavGraphFragmentContainerView).navigateUp()

}