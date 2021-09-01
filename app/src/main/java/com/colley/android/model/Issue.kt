package com.colley.android.model

class Issue(
    val issueId: String? = null,
    val userId: String? = null,
    val title: String? = null,
    val body: String? = null,
    val timeStamp: String? = null,
    val contributionsCount: Int = 0,
    val endorsementsCount: Int = 0
    )
