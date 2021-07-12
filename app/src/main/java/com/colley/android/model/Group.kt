package com.colley.android.model

class Group (
    val groupIcon: String? = null,
    val name: String,
    val unreadMessages: Int = 0
) {

    companion object {
        fun getListOfGroups(): ArrayList<Group> {
            return arrayListOf(
                Group(
                    "https://rebrand.ly/lre8sby",
                    "Basket ball team",
                    115),
                Group(
                    name = "Med.biochem 200L",
                    unreadMessages = 20),
                Group(
                    groupIcon = "https://rebrand.ly/uebxhcu",
                    name = "UNIBEN Aspirants group"),
                Group(
                    "https://rebrand.ly/5cmpwwe",
                    "NUESA National secretariat",
                    3)
            )
        }

    }

}
