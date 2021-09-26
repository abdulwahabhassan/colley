package com.colley.android.model

object DummyData {



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