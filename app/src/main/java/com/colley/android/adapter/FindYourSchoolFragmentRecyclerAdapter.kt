package com.colley.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.school.School
import com.colley.android.databinding.SchoolItemBinding

class FindYourSchoolFragmentRecyclerAdapter() :
    RecyclerView.Adapter<FindYourSchoolFragmentRecyclerAdapter.SchoolViewHolder>() {

    var listOfSchools = arrayListOf<School>()
    private lateinit var clickListener : ItemClickedListener

    interface ItemClickedListener {
        fun onItemClick(school : School)
    }

    class SchoolViewHolder(private val itemBinding : SchoolItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(school: School, clickListener: ItemClickedListener) = with(itemBinding) {
            schoolNameTextView.text = school.name
            Glide.with(this.root.context).load(school.logoUrl).into(schoolLogoImageView)

            this.root.setOnClickListener {
                clickListener.onItemClick(school)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolViewHolder {
        val viewBinding = SchoolItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SchoolViewHolder((viewBinding))
    }

    override fun onBindViewHolder(holder: SchoolViewHolder, position: Int) {
        val school = listOfSchools[position]
        holder.bind(school, clickListener)
    }

    override fun getItemCount(): Int {
       return listOfSchools.size
    }

    fun setList(list: ArrayList<School>, clickListener: ItemClickedListener) {
        this.listOfSchools = list
        this.clickListener = clickListener
        notifyDataSetChanged()
    }
}