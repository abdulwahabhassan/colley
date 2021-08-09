package com.colley.android.model

data class Group (
   var id: String? = "",
   var name: String? = "",
   var icon: String? = "",
   var admin: String? = "",
   var description: String? = "",
   var members: ArrayList<String?> = arrayListOf(),
   var messages: ArrayList<String?> = arrayListOf()
        ) {

}