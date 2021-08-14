package com.colley.android.view.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.colley.android.R
import com.colley.android.SaveButtonListener
import com.colley.android.databinding.FragmentHomeBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator


class HomeFragment : Fragment(), AddGroupBottomSheetDialogFragment.SaveButtonListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: FragmentStateAdapter
    lateinit var homeFab: FloatingActionButton
    lateinit var addGroupBottomSheetDialog: AddGroupBottomSheetDialogFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.main_activity_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
       return when (item.itemId) {
            R.id.search_menu_item -> {
                Toast.makeText(context, "Searching", Toast.LENGTH_LONG).show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
            private val fragments = arrayOf<Fragment>(
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
            when (viewPager.currentItem) {
                0 -> {
                    Snackbar.make(requireView(), "Write an issue", Snackbar.LENGTH_LONG).show()
                }
                1 -> {
                    Snackbar.make(requireView(), "Make a post", Snackbar.LENGTH_LONG).show()
                }
                2 -> {
                    addGroupBottomSheetDialog = AddGroupBottomSheetDialogFragment(this, requireContext(), requireView())
                    addGroupBottomSheetDialog.show(parentFragmentManager, null)

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onSave() {
        addGroupBottomSheetDialog.dismiss()
    }

}