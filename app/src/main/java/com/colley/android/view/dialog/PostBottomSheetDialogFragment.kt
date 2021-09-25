package com.colley.android.view.dialog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.colley.android.R
import com.colley.android.databinding.BottomSheetDialogFragmentPostBinding
import com.colley.android.model.Comment
import com.colley.android.view.fragment.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot

class PostBottomSheetDialogFragment (
    private val parentContext: Context,
    private val postView: View,
    private val postDialogListener: PostDialogListener
        ) :
    BottomSheetDialogFragment(),
    PostCommentBottomSheetDialogFragment.CommentListener{

    private var postId: String? = null
    private var _binding: BottomSheetDialogFragmentPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPagerAdapter: FragmentStateAdapter
    private lateinit var commentSheetDialog: PostCommentBottomSheetDialogFragment
    interface PostDialogListener {
        fun onCommented(currentData: DataSnapshot?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //retrieve post id from bundle
            postId = it.getString(POST_ID_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetDialogFragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initialize tabLayout
        tabLayout = binding.bottomSheetTabLayout
        //initialize viewpager
        viewPager = binding.bottomSheetViewPager

        //set up viewPager
        viewPagerAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            private val fragments = arrayOf(
                PostCommentsFragment(postId, parentContext, postView),
                LikesFragment(postId, parentContext, postView),
                PromotionsFragment(postId, parentContext, postView)
            )

            override fun createFragment(position: Int) = fragments[position]

            override fun getItemCount(): Int = fragments.size
        }

        //bind to adapter
        viewPager.adapter = viewPagerAdapter

        //set up and sync viewpager with tabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.comments_tab_name)
                1 -> getString(R.string.likes_tab_name)
                else -> getString(R.string.promotions_tab_name)
            }
        }.attach()

        binding.commentImageView.setOnClickListener {
            commentSheetDialog = PostCommentBottomSheetDialogFragment(
                requireContext(),
                requireView(),
                this
            )
            commentSheetDialog.arguments = bundleOf("postIdKey" to postId)
            commentSheetDialog.show(parentFragmentManager, null)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val POST_ID_KEY = "postIdKey"
    }

    override fun onComment(currentData: DataSnapshot?) {
        postDialogListener.onCommented(currentData)
    }

}