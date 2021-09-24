package com.colley.android.adapter

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.colley.android.view.fragment.CommentsFragment
import com.colley.android.view.fragment.LikesFragment
import com.colley.android.view.fragment.PromotionsFragment

class PostBottomSheetDialogFragmentViewPager(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val postId: String?,
    private val parentContext: Context,
    private val postView: View
)
    : FragmentStateAdapter(fragmentManager, lifecycle){
    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> LikesFragment(postId, parentContext, postView)
            2 -> PromotionsFragment(postId, parentContext, postView)
            else -> CommentsFragment(postId, parentContext, postView)
        }
    }

    companion object {
        private const val NUM_PAGES = 3
    }
}