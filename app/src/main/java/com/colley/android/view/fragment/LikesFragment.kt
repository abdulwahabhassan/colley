package com.colley.android.view.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.LikesFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentLikesBinding
import com.colley.android.model.DummyData
import com.colley.android.model.Like

class LikesFragment(
    private val postId: String?,
    private val parentContext: Context,
    private val postView: View) : Fragment(), LikesFragmentRecyclerAdapter.ItemClickedListener{

    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.likesRecyclerView
        val recyclerViewAdapter = LikesFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfLikes())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(like: Like) {

    }
}