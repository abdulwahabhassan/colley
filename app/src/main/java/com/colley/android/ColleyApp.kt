package com.colley.android

import android.app.Application
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ColleyApp : Application() {
    override fun onCreate() {
        super.onCreate()
//        Firebase.database.setPersistenceEnabled(true)
    }
}