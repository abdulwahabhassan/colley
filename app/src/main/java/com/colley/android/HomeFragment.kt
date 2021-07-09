package com.colley.android

import android.net.wifi.hotspot2.pps.HomeSp
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.colley.android.adapter.HomeFragmentViewPagerAdapter
import com.colley.android.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayout


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPagerAdapter: HomeFragmentViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.homeFragmentViewPager
        tabLayout = binding.homeFragmentTabLayout
        viewPagerAdapter = HomeFragmentViewPagerAdapter(childFragmentManager, lifecycle)
        viewPager.adapter = viewPagerAdapter

        tabLayout.addTab( tabLayout.newTab().setText(getString(R.string.issues_tab_name)))
        tabLayout.addTab( tabLayout.newTab().setText(getString(R.string.posts_tab_name)))
        tabLayout.addTab( tabLayout.newTab().setText(getString(R.string.groups_tab_name)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
//                childFragmentManager.popBackStack()
                binding.homeFragmentViewPager.currentItem = tab?.position ?: 0
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