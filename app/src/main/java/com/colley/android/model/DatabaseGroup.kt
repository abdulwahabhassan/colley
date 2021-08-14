package com.colley.android.model

data class DatabaseGroup (
   var id: String?,
   var name: String?,
   var photo: String?,
   var admins: String?,
   var description: String?,
   var members: ArrayList<String?> = arrayListOf(),
   var messages: ArrayList<String?> = arrayListOf()
        ) {

}