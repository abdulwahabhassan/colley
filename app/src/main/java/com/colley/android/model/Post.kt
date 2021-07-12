package com.colley.android.model

class Post (
    val userPhoto: String,
    val name: String,
    val school: String,
    val timeStamp: String,
    val location: String? = null,
    val text: String? = null,
    val image: String? = null,
    val likes: Int? = 0,
    val comments: Int? = 0,
    val promotions: Int? = 0) {

    companion object {
        fun getListOfPosts(): ArrayList<Post> {
            return arrayListOf(
                Post(
                    userPhoto = "https://rebrand.ly/zlhfnjj",
                    "Omowumi Abimbola",
                    "University of Ibadan",
                    "Just now",
                    "Queen Elizabeth hall",
                    "Last class for today just ended.. Friday vibes!",
                    likes = 0,
                    comments = 0,
                    promotions = 1 ),
                Post(
                    userPhoto = "https://rebrand.ly/00p0y9v",
                    "Ella David Andrews",
                    "University of Uyo",
                    "16 minutes ago",
                    text = "Today's edition of the Right Platform focuses on Eket local government area. " +
                            "It x-rays the ongoing rural development, urban renewal, and the revamp in sporting and healthcare delivery. \n" +
                            "Join us today on Planet Radio 101.1FM by 10am and on AKBC TV by 8.30pm\n\n" +
                            "Powered by Mandate Eyes",
                    image = "https://rebrand.ly/andoaru",
                    likes = 19,
                    comments = 4,
                    promotions = 1 ),
                Post(
                    userPhoto = "https://rebrand.ly/cc685c",
                    "Hussena Abdulwahab",
                    "Kaduna Polytechnic",
                    "2 hours ago",
                    "Admin building",
                    text = "Hi everyone, let's be friends",
                    likes = 125,
                    comments = 43,
                    promotions = 0 ),
                Post(
                    userPhoto = "https://rebrand.ly/bfdcfd",
                    "Alasa Umoru",
                    "University of Benin",
                    "Yesterday",
                    "Basement",
                    "Android developers workshop ongoing at Basement",
                    "https://rebrand.ly/dp427gf",
                    likes = 10,
                    comments = 0,
                    promotions = 9)
            )
        }
    }

}
