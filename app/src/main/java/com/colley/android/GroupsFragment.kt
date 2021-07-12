package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.GroupsFragmentRecyclerAdapter
import com.colley.android.adapter.PostsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentGroupsBinding
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.model.Group
import com.colley.android.model.Post


class GroupsFragment : Fragment(), GroupsFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.groupRecyclerView
        val recyclerViewAdapter = GroupsFragmentRecyclerAdapter()
        recyclerViewAdapter.setList(Group.getListOfGroups(), this)
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onItemClick(group: Group) {
    }

}