package com.colley.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.CommentsBottomSheetRecyclerAdapter
import com.colley.android.databinding.BottomSheetDialogFragmentCommentsBinding
import com.colley.android.model.Comment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommentsBottomSheetDialog (private val bottomSheetCommentListener: BottomSheetCommentListener) :
    BottomSheetDialogFragment(), CommentsBottomSheetRecyclerAdapter.ItemClickedListener {

    private var _binding: BottomSheetDialogFragmentCommentsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    //This will be used to listen for new comments and pass the comment to whichever fragments
    //implements this interface
    interface BottomSheetCommentListener {
        fun addComment(text: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetDialogFragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.bottomSheetDialogCommentsRecyclerView
        val recyclerViewAdapter = CommentsBottomSheetRecyclerAdapter(this)
        recyclerViewAdapter.setList(Comment.getListOfComments())
        recyclerView.adapter = recyclerViewAdapter

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(comment: Comment) {

    }


}