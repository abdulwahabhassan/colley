package com.colley.android.model

data class NewGroup (
    var name: String? = null,
    var description: String? = null,
    var groupPhoto: String? = null,
    var groupAdmins: ArrayList<String> = arrayListOf(),
    var members: ArrayList<String> = arrayListOf()
        )