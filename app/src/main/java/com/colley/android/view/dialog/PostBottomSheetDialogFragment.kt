package com.colley.android.view.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.colley.android.R
import com.colley.android.adapter.PostBottomSheetDialogFragmentViewPager
import com.colley.android.databinding.BottomSheetDialogFragmentPostBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class PostBottomSheetDialogFragment () :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetDialogFragmentPostBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPagerAdapter: PostBottomSheetDialogFragmentViewPager



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
        viewPager = binding.bottomSheetViewPager
        tabLayout = binding.bottomSheetTabLayout
        viewPagerAdapter = PostBottomSheetDialogFragmentViewPager(childFragmentManager, lifecycle)
        viewPager.adapter = viewPagerAdapter

        tabLayout.addTab( tabLayout.newTab().setText(getString(R.string.comments_tab_name)))
        tabLayout.addTab( tabLayout.newTab().setText(getString(R.string.likes_tab_name)))
        tabLayout.addTab( tabLayout.newTab().setText(getString(R.string.promotions_tab_name)))


        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                    binding.bottomSheetViewPager.currentItem = tab?.position ?: 0
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }



}