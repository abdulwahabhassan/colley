package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.GroupsFragmentRecyclerAdapter
import com.colley.android.adapter.PostsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentGroupsBinding
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.model.DummyData
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
        val recyclerViewAdapter = GroupsFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfGroups())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onItemClick(group: Group) {
        val groupName = group.name
        val action = HomeFragmentDirections.actionHomeFragmentToGroupChatFragment(groupName)
        parentFragment?.findNavController()?.navigate(action)
        //Toast.makeText(requireContext(), "Moving", Toast.LENGTH_LONG).show()
    }

}