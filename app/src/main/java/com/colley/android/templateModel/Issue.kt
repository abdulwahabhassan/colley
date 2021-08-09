package com.colley.android.templateModel

class Issue(
    val title: String,
    val body: String,
    val timeStamp: String,
    val contributionsCount: Int = 0,
    val endorsementsCount: Int = 0,
    val userName: String,
    val userSchool: String,
    val userPhoto: String? = null
    )
