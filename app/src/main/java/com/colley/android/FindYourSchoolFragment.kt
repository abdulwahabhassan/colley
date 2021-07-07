package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.FindYourSchoolFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentFindYourSchoolBinding
import com.colley.android.school.School

class FindYourSchoolFragment : Fragment(), FindYourSchoolFragmentRecyclerAdapter.ItemClickedListener {
    private var _binding: FragmentFindYourSchoolBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFindYourSchoolBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.findYourSchoolRecyclerView
        val recyclerViewAdapter = FindYourSchoolFragmentRecyclerAdapter()
        recyclerViewAdapter.setList(School.getListOfSchools(), this)
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(school: School) {
        val schoolBundle = bundleOf(getString(R.string.school_key) to school.name)
        findNavController().navigate(R.id.action_findYourSchoolFragment_to_homeFragment, schoolBundle)
    }

}