package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.GroupsFragmentRecyclerAdapter
import com.colley.android.adapter.IssuesFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentGroupsBinding
import com.colley.android.databinding.FragmentIssuesBinding
import com.colley.android.model.Group
import com.colley.android.model.Issue


class IssuesFragment : Fragment(), IssuesFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentIssuesBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIssuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.issueRecyclerView
        val recyclerViewAdapter = IssuesFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(Issue.getListOfIssues())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onItemClick(issue: Issue) {

    }

}