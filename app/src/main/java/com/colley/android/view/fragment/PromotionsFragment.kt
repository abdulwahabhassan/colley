package com.colley.android.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.PromotionsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentPromotionsBinding
import com.colley.android.templateModel.DummyData
import com.colley.android.templateModel.Promotion

class PromotionsFragment : Fragment(), PromotionsFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentPromotionsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPromotionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.promotionsRecyclerView
        val recyclerViewAdapter = PromotionsFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfPromotions())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(promotion: Promotion) {

    }

}