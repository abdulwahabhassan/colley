package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.databinding.ItemPostBinding
import com.colley.android.databinding.ItemPromotionBinding
import com.colley.android.model.*

class PromotionsFragmentRecyclerAdapter(private val clickListener: ItemClickedListener) : RecyclerView.Adapter<PromotionsFragmentRecyclerAdapter.PromotionViewHolder>(){

    var listOfPromotions = arrayListOf<Promotion>()

    interface ItemClickedListener {
        fun onItemClick(promotion: Promotion)
    }

    class PromotionViewHolder(private val itemBinding : ItemPromotionBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(promotion: Promotion, clickListener: ItemClickedListener) = with(itemBinding) {

            this.root.setOnClickListener {
                clickListener.onItemClick(promotion)
            }
        }
    }

    fun setList(list: ArrayList<Promotion>) {
        this.listOfPromotions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromotionViewHolder {
        val viewBinding = ItemPromotionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromotionViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: PromotionViewHolder, position: Int) {
        val promotion = listOfPromotions[position]
        holder.bind(promotion, clickListener)
    }

    override fun getItemCount(): Int {
        return listOfPromotions.size
    }
}