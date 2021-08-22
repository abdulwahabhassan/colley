package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.IssuesFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentIssuesBinding
import com.colley.android.model.DummyData
import com.colley.android.model.Issue


class IssuesFragment : Fragment(), IssuesFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentIssuesBinding? = null
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
        _binding = FragmentIssuesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.issueRecyclerView
        val recyclerViewAdapter = IssuesFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfIssues())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_menu_item -> {
                Toast.makeText(context, "Search in issues", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(issue: Issue) {

    }

}