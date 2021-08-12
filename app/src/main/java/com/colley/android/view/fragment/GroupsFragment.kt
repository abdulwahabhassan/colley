package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.group.GroupsRecyclerAdapter
import com.colley.android.databinding.FragmentGroupsBinding
import com.colley.android.templateModel.DummyData
import com.colley.android.templateModel.Group


class GroupsFragment : Fragment(), GroupsRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        val recyclerViewAdapter = GroupsRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfGroups())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_menu_item -> {
                Toast.makeText(context, "Search in groups", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(group: Group) {
        val groupName = group.name
        val action = HomeFragmentDirections.actionHomeFragmentToGroupChatFragment(groupName)
        parentFragment?.findNavController()?.navigate(action)
        //Toast.makeText(requireContext(), "Moving", Toast.LENGTH_LONG).show()
    }

}