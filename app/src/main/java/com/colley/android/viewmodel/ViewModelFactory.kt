package com.colley.android.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner

//Factory for ViewModels
class ViewModelFactory(
        owner: SavedStateRegistryOwner,
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(PostsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostsViewModel(handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}