package com.colley.android.model

class Issue(
    val title: String,
    val body: String,
    val timeStamp: String,
    val contributionsCount: Int = 0,
    val endorsementsCount: Int = 0,
    val userName: String,
    val userSchool: String,
    val userPhoto: String? = null
    ) {

    companion object {
        fun getListOfIssues() : ArrayList<Issue> {
            return arrayListOf(
                Issue(
                    "Let's talk about the malpractice",
                    "Examination malpractice is as old as examination itself. However, " +
                            "the rate at which examination malpractices occurs in the Nigerian educational " +
                            "system is highly disturbing. The challenge therefore needs prompt " +
                            "attention. The phenomenon which has both moral and legal dimensions is " +
                            "considered as a hydra-headed problem that has pervaded the entire " +
                            "educational system in Nigeria.",
                    "Just now",
                    2,
                    0,
                    "Olaniyi Anslem",
                    "LAUTECH",
                    "https://rebrand.ly/bfdcfd"
                    ),
                Issue(
                    "Bank road is not safe to walk on at the moment",
                    "An ongoing robbery at bank road! please spread the word!!!",
                    "45 minutes ago",
                    114,
                    1_219,
                    "Maro Godwin",
                    "AAU",
                    "https://rebrand.ly/zlhfnjj"),
                Issue(
                    "Rector versus students of AUCHIPOLY",
                    "All allegations of corruption that we brought against the rector " +
                            "have been proven false. Where does this leave us now? I fear that we " +
                            "have brought destruction upon ourself with our own hands. Could there be " +
                            "more to the story than we already know?",
                    "6 hours ago",
                    486,
                    72,
                    "Ginika Ofor",
                    "AUCHIPOLY",
                    "https://rebrand.ly/00p0y9v"),
                Issue(
                    "ASUU strike looming", "Word on the street says that ASUU is cooking " +
                            "another indefinite strike. Wahala be like ASUU.",
                    "1 minute ago",
                    0,
                    0,
                    "Linus Meganus",
                    "ABSU",
                    ),
                Issue(
                    "Can I switch from Agric to Life Sciences in 200L?",
                    "I have always wanted to study Biochemistry. But the university ended up giving " +
                            "me Agric. I was told that in my 2nd year, with good grades, I could apply for a " +
                            "transfer. However I want to be sure of this. I will appreciate any contributions.",
                    "2 days ago",
                    33,
                    1,
                    "Ada Love",
                    "UNILAG",
                    "https://rebrand.ly/cc685c"
                ),
                Issue(
                    "Let's talk about the malpractice",
                    "Examination malpractice is as old as examination itself. However, " +
                            "the rate at which examination malpractices occurs in the Nigerian educational " +
                            "system is highly disturbing. The challenge therefore needs prompt " +
                            "attention. The phenomenon which has both moral and legal dimensions is " +
                            "considered as a hydra-headed problem that has pervaded the entire " +
                            "educational system in Nigeria.",
                    "Just now",
                    2,
                    0,
                    "Olaniyi Anslem",
                    "LAUTECH",
                    "https://rebrand.ly/bfdcfd"
                ),
                Issue(
                    "Bank road is not safe to walk on at the moment",
                    "An ongoing robbery at bank road! please spread the word!!!",
                    "45 minutes ago",
                    114,
                    1_219,
                    "Maro Godwin",
                    "AAU",
                    "https://rebrand.ly/zlhfnjj"),
                Issue(
                    "Rector versus students of AUCHIPOLY",
                    "All allegations of corruption that we brought against the rector " +
                            "have been proven false. Where does this leave us now? I fear that we " +
                            "have brought destruction upon ourself with our own hands. Could there be " +
                            "more to the story than we already know?",
                    "6 hours ago",
                    486,
                    72,
                    "Ginika Ofor",
                    "AUCHIPOLY",
                    "https://rebrand.ly/00p0y9v"),
                Issue(
                    "ASUU strike looming", "Word on the street says that ASUU is cooking " +
                            "another indefinite strike. Wahala be like ASUU.",
                    "1 minute ago",
                    0,
                    0,
                    "Linus Meganus",
                    "ABSU",
                )
            )
        }

    }
}
