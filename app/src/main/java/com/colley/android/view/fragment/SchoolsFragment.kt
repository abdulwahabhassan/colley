package com.colley.android.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.SchoolsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentSchoolsBinding
import com.colley.android.model.DummyData
import com.colley.android.model.School

class SchoolsFragment : Fragment(), SchoolsFragmentRecyclerAdapter.ItemClickedListener {
    private var _binding: FragmentSchoolsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSchoolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.schoolRecyclerView
        val recyclerViewAdapter = SchoolsFragmentRecyclerAdapter()
        recyclerViewAdapter.setList(DummyData.getListOfSchools(), this)
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(school: School) {
        //Using safe args to navigate and pass data between two destinations
        val action = SchoolsFragmentDirections.actionFindYourSchoolFragmentToSignupFragment(school.name)
        findNavController().navigate(action)
    }

}