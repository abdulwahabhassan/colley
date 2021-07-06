package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.FindYourSchoolRecyclerAdapter
import com.colley.android.databinding.FragmentFindYourSchoolBinding
import com.colley.android.school.School

class FindYourSchoolFragment : Fragment() {
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
        val recyclerViewAdapter = FindYourSchoolRecyclerAdapter()
        recyclerViewAdapter.setList(School.getListOfSchools())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}