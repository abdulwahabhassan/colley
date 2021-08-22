package com.colley.android.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.CommentsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentCommentsBinding
import com.colley.android.model.Comment
import com.colley.android.model.DummyData

class CommentsFragment : Fragment(), CommentsFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.commentsRecyclerView
        val recyclerViewAdapter = CommentsFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfComments())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(comment: Comment) {

    }
}