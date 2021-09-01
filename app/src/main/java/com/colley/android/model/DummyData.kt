package com.colley.android.model

object DummyData {

    fun getListOfFriends(): ArrayList<GroupMember> {
        return arrayListOf(
            GroupMember(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
                false
            ),
            GroupMember(
                "Akpos Godfrey",
                "https://rebrand.ly/zlhfnjj",
                false
            ),
            GroupMember(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
                false
            ),
            GroupMember(
                "Patricia Benedict",
                "https://rebrand.ly/cc685c",
                false
            ),
            GroupMember(
                "Akpos Godfrey",
                null,
                false
            ),
            GroupMember(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
                false
            )
        )
    }

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
                userPhoto = "https://rebrand.ly/amrrend",
                "Fatima Abdulwahab",
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

    fun getListOfSchools(): ArrayList<School> {
        return arrayListOf(
            School("Abia State University",
                "https://rebrand.ly/4ef505"),
            School("American University of Nigeria", "https://rebrand.ly/xzk0q0b"),
            School("Achievers University", "https://rebrand.ly/dmnmyr3"),
            School("Ambrose Alli University", "https://rebrand.ly/9ltfw0z"),
            School("Benue State University", "https://rebrand.ly/2o7dmly"),
            School("University of Ibadan", "https://rebrand.ly/wrwd07h"),
            School("National Open University of Nigeria", "https://rebrand.ly/r4wmpyt"),
            School("Crescent University", "https://rebrand.ly/vc0gtml"),
            School("Delta State University, Abraka", "https://www.delsu.edu.ng/images/12.png"),
            School("Ebonyi State University", "https://rebrand.ly/5jcux2a"),
            School("Michael Okpara University of Agriculture", "https://rebrand.ly/ika41u1"),
            School("University of Uyo", "https://rebrand.ly/rd8r0uy"),
            School("University of Agriculture, Makurdi", "https://rebrand.ly/h4n434r"),
            School("The Polytechnic, Ibadan", "https://rebrand.ly/t6jio7f"),
            School("Federal University of Petroleum Resources", "https://rebrand.ly/fk9co9i")
        )
    }

    fun getListOfLikes(): ArrayList<Like> {
        return arrayListOf(
            Like(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
            ),
            Like(
                "Emmanuel Campbell",
                "https://rebrand.ly/bfdcfd",
            ),
            Like(
                "Emmanuel Campbell",
                "https://rebrand.ly/bfdcfd",
                ),
            Like(
                "Patricia Benedict",
                "https://rebrand.ly/cc685c",
            ),
            Like(
                "Akpos Godfrey",
                userPhoto = null,
            ),
            Like(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
            ),
            Like(
                "Akpos Godfrey",
                "https://rebrand.ly/zlhfnjj",
            ),
            Like(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
                ),
            Like(
                "Patricia Benedict",
                "https://rebrand.ly/cc685c",
                ),
            Like(
                "Akpos Godfrey",
                userPhoto = null,
                ),
            Like(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
                ),
            Like(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
            ),
            Like(
                "Emmanuel Campbell",
                "https://rebrand.ly/bfdcfd",
            ),
            Like(
                "Emmanuel Campbell",
                "https://rebrand.ly/bfdcfd",
                ),
            Like(
                "Akpos Godfrey",
                "https://rebrand.ly/zlhfnjj",
                ),
            Like(
                "Patricia Benedict",
                "https://rebrand.ly/cc685c",
               ),
            Like(
                "Akpos Godfrey",
                userPhoto = null,
                )
        )
    }

    fun getListOfPromotions(): ArrayList<Promotion> {
        return arrayListOf(
            Promotion(
                "Mirabell Sanchez Ozioma",
                "https://rebrand.ly/00p0y9v",
            ),
            Promotion(
                "Akpos Godfrey",
                userImage = null,
            ),
            Promotion(
                "Emmanuel Campbell",
                "https://rebrand.ly/bfdcfd",
            ),
            Promotion(
                "Patricia Benedict",
                "https://rebrand.ly/cc685c",
            )
        )
    }

    fun getListOfNotifications(): ArrayList<Notification> {
        return arrayListOf(
            Notification(
                "https://rebrand.ly/00p0y9v",
                "Ella Andrews from UNIUYO commented on your post",
                "Just now"
            ),
            Notification(
                image = null,
                "Alice Angel from FUTO promoted your post",
                "3min ago"
            ),
            Notification(
                "https://rebrand.ly/bfdcfd",
                "Bernard Matthew from UNICAL contributed to this issue - Let’s talk about the malpractice",
                "1hr ago"
            ),
            Notification(
                "https://rebrand.ly/cc685c",
                "Zane Tega from BABCOCK commented on your contribution to this issue - " +
                        "Bank road is not safe to walk on at the moment",
                "4hr ago"
            ),
            Notification(
                "https://rebrand.ly/cc685c",
                "Zane Tega from BABCOCK liked your contribution on this issue - Bank road is not safe to walk on at the moment",
                "4hr ago"
            ),
            Notification(
                "https://rebrand.ly/zlhfnjj",
                "Timidayo Bawo from UNN responded to your comment on Alfred Chigbo’s post",
                "yd"
            ),
            Notification(
                image = null,
                "Alfred Chigbo from UNIMAID liked your comment on his post",
                "3dy ago"
            )
        )
    }

}