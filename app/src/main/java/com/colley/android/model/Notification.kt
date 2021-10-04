package com.colley.android.model

data class Notification (
    var itemId: String? = null,
    var itemOwnerUserId: String? = null,
    var itemActorUserId: String? = null,
    var timeId: Long? = null,
    var timeStamp: String? = null,
    var itemActionId: String? = null,
    var itemType: String? = null,
    var itemActionType: String? = null
        )

//item could be a post or issue
//item owner is whoever raised the issue or made the post
//item actor is whoever acted on the item
//item action could be a comment or like or endorsement