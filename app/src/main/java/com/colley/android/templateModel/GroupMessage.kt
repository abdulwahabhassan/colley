package com.colley.android.templateModel

class GroupMessage {

    var text: String? = null
    var name: String? = null
    var photoUrl: String? = null
    var userPhoto: String? = null

    // Empty constructor needed for Firestore serialization
    constructor()

    constructor(text: String?, name: String?, photoUrl: String?, userPhoto: String?) {
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
        this.userPhoto = userPhoto
    }

}
