package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.PostsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.model.DummyData
import com.colley.android.model.Post


class PostsFragment : Fragment(),
    PostsFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.postRecyclerView
        val recyclerViewAdapter = PostsFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfPosts())
        recyclerView.adapter = recyclerViewAdapter
    }


    override fun onItemClick(post: Post) {
        val bottomSheetDialog = PostBottomSheetDialogFragment()
        bottomSheetDialog.show(childFragmentManager, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}