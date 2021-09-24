package com.colley.android.model

data class Post (
    val postId: String? = null,
    val userId: String? = null,
    val timeStamp: String? = null,
    val timeId: Long? = null,
    val location: String? = null,
    val text: String? = null,
    val image: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val promotionsCount: Int = 0) {

}
