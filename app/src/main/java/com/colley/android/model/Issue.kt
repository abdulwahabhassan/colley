package com.colley.android.model

data class Issue(
    val issueId: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val body: String? = null,
    val timeStamp: String? = null,
    var contributionsCount: Int = 0,
    var endorsementsCount: Int = 0
    )
