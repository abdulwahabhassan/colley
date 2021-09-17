package com.colley.android.model

data class Post (
    val postId: String? = null,
    val userId: String? = null,
    val timeStamp: String? = null,
    val location: String? = null,
    val text: String? = null,
    val image: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val promotions: Int = 0) {

}
