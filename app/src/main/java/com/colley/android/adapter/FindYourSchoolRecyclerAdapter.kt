package com.colley.android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.school.School
import com.colley.android.databinding.SchoolItemBinding

class FindYourSchoolRecyclerAdapter() :
    RecyclerView.Adapter<FindYourSchoolRecyclerAdapter.SchoolViewHolder>() {

    var listOfSchools = arrayListOf<School>()

    class SchoolViewHolder(private val itemBinding : SchoolItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(school: School) = with(itemBinding) {
            schoolNameTextView.text = school.name
            Glide.with(this.root.context).load(school.logoUrl).into(schoolLogoImageView)
        }
    }

    fun setList(list: ArrayList<School>) {
        this.listOfSchools = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolViewHolder {
        val viewBinding = SchoolItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SchoolViewHolder((viewBinding))
    }

    override fun onBindViewHolder(holder: SchoolViewHolder, position: Int) {
        val school = listOfSchools[position]
        holder.bind(school)
    }

    override fun getItemCount(): Int {
       return listOfSchools.size
    }
}