package com.colley.android.wrapper

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler


//Fixes IndexOutOfBoundsException that occurs when trying to come back to fragment
class WrapContentLinearLayoutManager(
    context: Context,
    horizontal: Int,
    b: Boolean
) : LinearLayoutManager(context, horizontal, b) {
    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) { }
    }
}