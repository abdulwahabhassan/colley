package com.colley.android.model

class GroupMessage(
    var userId: String? = null,
    var text: String? = null,
    var image: String? = null
        ) {

//    var text: String? = ""
//    var name: String? = ""
//    var photoUrl: String? = ""
//    var userPhoto: String? = ""
//    var userId: String? = ""

    // Empty constructor needed for Firestore serialization
//    constructor()

//    constructor(text: String?, name: String?, photoUrl: String?, userPhoto: String?, userId: String?) {
//        this.text = text
//        this.name = name
//        this.photoUrl = photoUrl
//        this.userPhoto = userPhoto
//        this.userId = userId
//    }

}
