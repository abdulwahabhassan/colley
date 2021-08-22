package com.colley.android.view.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.R
import com.colley.android.adapter.PostsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.model.DummyData
import com.colley.android.model.Post


class PostsFragment : Fragment(),
    PostsFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentPostsBinding? = null
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_menu_item -> {
                Toast.makeText(context, "Search in posts", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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