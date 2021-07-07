package com.colley.android.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.colley.android.GroupsFragment
import com.colley.android.IssuesFragment
import com.colley.android.PostsFragment

class HomeFragmentViewPagerAdapter(fragmentManager: FragmentManager, lifecycle : Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> PostsFragment()
            2 -> GroupsFragment()
            else -> IssuesFragment()
        }
    }
    companion object {
        private const val NUM_PAGES = 3
    }

}