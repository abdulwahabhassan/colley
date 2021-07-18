package com.colley.android.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.colley.android.*

class PostBottomSheetDialogFragmentViewPager(fragmentManager: FragmentManager, lifecycle : Lifecycle)
    : FragmentStateAdapter(fragmentManager, lifecycle){
    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> LikesFragment()
            2 -> PromotionsFragment()
            else -> CommentsFragment()
        }
    }

    companion object {
        private const val NUM_PAGES = 3
    }
}