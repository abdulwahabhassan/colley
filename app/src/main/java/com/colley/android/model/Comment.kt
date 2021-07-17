package com.colley.android.model

class Comment(val commentText: String,
              val commenterName: String,
              val commenterPhoto: String? = null,
              val commentTimeStamp: String) {

    companion object{
        fun getListOfComments(): ArrayList<Comment> {
            return arrayListOf(
                Comment(
                    "I am personally of the opinion that caution should be taken",
                    "Emmanuel Campbell",
                    "https://rebrand.ly/bfdcfd",
                    "Just now"),
                Comment(
                    "Wahala be like bicyle. This issue no suppose long pass as e don be so " +
                            "I no like say we still dey drag this matter. Na wa ooh.. shuu.. Shey we " +
                            "no go decide move ahead make the rest meet us for front. Na waka me I dey so oh",
                    "Akpos Godfrey",
                    "https://rebrand.ly/zlhfnjj",
                    "37min"
                ),
                Comment(
                    "Hahahhaha.. I can't laugh alone. Apostle must hear this!",
                    "Mirabell Sanchez Ozioma",
                    "https://rebrand.ly/00p0y9v",
                    "1hr"),
                Comment(
                    "***giggling***",
                    "Patricia Benedict",
                    "https://rebrand.ly/cc685c",
                    "1d"
                ),
                Comment(
                    "Wahala be like bicyle. This issue no suppose long pass as e don be so " +
                            "I no like say we still dey drag this matter. Na wa ooh.. shuu.. Shey we " +
                            "no go decide move ahead make the rest meet us for front. Na waka me I dey so oh",
                    "Akpos Godfrey",
                    commenterPhoto = null,
                    "8months"
                ),
                Comment(
                    "Hahahhaha.. I can't laugh alone. Apostle must hear this!",
                    "Mirabell Sanchez Ozioma",
                    "https://rebrand.ly/00p0y9v",
                    "1hr"),
                Comment(
                    "I am personally of the opinion that caution should be taken",
                    "Emmanuel Campbell",
                    "https://rebrand.ly/bfdcfd",
                    "Just now"),
                Comment(
                    "Wahala be like bicyle. This issue no suppose long pass as e don be so " +
                            "I no like say we still dey drag this matter. Na wa ooh.. shuu.. Shey we " +
                            "no go decide move ahead make the rest meet us for front. Na waka me I dey so oh",
                    "Akpos Godfrey",
                    "https://rebrand.ly/zlhfnjj",
                    "1yr"
                ),
                Comment(
                    "***giggling***",
                    "Patricia Benedict",
                    "https://rebrand.ly/cc685c",
                    "1d"
                ),
                Comment(
                    "Wahala be like bicyle. This issue no suppose long pass as e don be so " +
                            "I no like say we still dey drag this matter. Na wa ooh.. shuu.. Shey we " +
                            "no go decide move ahead make the rest meet us for front. Na waka me I dey so oh",
                    "Akpos Godfrey",
                    commenterPhoto = null,
                    "8months"
                ),
                Comment(
                    "Hahahhaha.. I can't laugh alone. Apostle must hear this!",
                    "Mirabell Sanchez Ozioma",
                    "https://rebrand.ly/00p0y9v",
                    "1hr"),
                Comment(
                    "I am personally of the opinion that caution should be taken",
                    "Emmanuel Campbell",
                    "https://rebrand.ly/bfdcfd",
                    "Just now")
            )
        }
    }
}