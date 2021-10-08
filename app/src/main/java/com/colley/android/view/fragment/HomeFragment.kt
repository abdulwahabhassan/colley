package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.*
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.colley.android.R
import com.colley.android.databinding.FragmentHomeBinding
import com.colley.android.view.dialog.AddGroupBottomSheetDialogFragment
import com.colley.android.view.dialog.NewPostBottomSheetDialogFragment
import com.colley.android.view.dialog.RaiseIssueBottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator


class HomeFragment:
    Fragment(),
    RaiseIssueBottomSheetDialogFragment.NewIssueListener,
    RaiseIssueBottomSheetDialogFragment.RaiseIssueHomeFabListener,
    NewPostBottomSheetDialogFragment.NewPostHomeFabListener,
    AddGroupBottomSheetDialogFragment.AddGroupFabListener{

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: FragmentStateAdapter
    lateinit var homeFab: FloatingActionButton
    private lateinit var addGroupBottomSheetDialog: AddGroupBottomSheetDialogFragment
    private lateinit var raiseIssueBottomSheetDialog: RaiseIssueBottomSheetDialogFragment
    private lateinit var newPostBottomSheetDialog: NewPostBottomSheetDialogFragment


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //initiate fab
        homeFab = binding.homeFab

        //set up viewpager
        viewPager = binding.homeFragmentViewPager
        viewPagerAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
            private val fragments = arrayOf(
                    IssuesFragment(),
                    PostsFragment(),
                    GroupsFragment(),
            )

            override fun createFragment(position: Int) = fragments[position]

            override fun getItemCount(): Int = fragments.size
        }

        //programmatically assign fab button drawable depending on which fragment is in view
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(homeFab.visibility == GONE) homeFab.visibility = VISIBLE
                when (position) {
                    0 -> homeFab.setImageResource(R.drawable.ic_issues)
                    1 -> homeFab.setImageResource(R.drawable.ic_post)
                    else -> homeFab.setImageResource(R.drawable.ic_add_group)
                }
            }
        })

        with(binding) {
            viewPager.adapter = viewPagerAdapter

            //set up and sync viewpager with tabLayout
            TabLayoutMediator(homeFragmentTabLayout, viewPager) { tab, position ->
                tab.text = when(position) {
                    0 -> getString(R.string.issues_tab_name)
                    1 -> getString(R.string.posts_tab_name)
                    else -> getString(R.string.groups_tab_name)
                }
            }.attach()
        }

        binding.homeFab.setOnClickListener {
            disableFab()
            when (viewPager.currentItem) {
                0 -> {
                    raiseIssueBottomSheetDialog = RaiseIssueBottomSheetDialogFragment(
                        requireContext(),
                        this,
                        this
                    )
                    raiseIssueBottomSheetDialog.show(parentFragmentManager, null)
                }
                1 -> {
                    newPostBottomSheetDialog = NewPostBottomSheetDialogFragment(
                        requireContext(),
                        requireView(),
                        this)
                    newPostBottomSheetDialog.show(parentFragmentManager, null)
                }
                2 -> {
                    addGroupBottomSheetDialog = AddGroupBottomSheetDialogFragment(
                        requireContext(),
                        requireView(),
                        this)
                    addGroupBottomSheetDialog.show(parentFragmentManager, null)

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun navigateToIssue(issueId: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToViewIssueFragment(issueId)
        findNavController().navigate(action)
    }

    override fun enableFab(enabled: Boolean) {
        binding.homeFab.isEnabled = enabled
    }

    //disable fab to prevent double clicks
    private fun disableFab() {
        binding.homeFab.isEnabled = false
    }


}