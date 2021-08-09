package com.colley.android.model

import com.google.firebase.database.IgnoreExtraProperties

data class Profile (
    var name: String? =  "",
    var school: String? = "",
    var course: String? = "",
    var role: String? = ""
        )
