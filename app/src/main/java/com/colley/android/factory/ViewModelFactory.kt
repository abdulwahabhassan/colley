package com.colley.android.factory

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.colley.android.repository.DatabaseRepository
import com.colley.android.viewmodel.IssuesViewModel
import com.colley.android.viewmodel.PostCommentsViewModel
import com.colley.android.viewmodel.PostsViewModel
import com.colley.android.viewmodel.ViewIssueViewModel

//Factory for ViewModels
class ViewModelFactory(
        owner: SavedStateRegistryOwner,
        private val repository: DatabaseRepository
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        when {
            modelClass.isAssignableFrom(PostsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return PostsViewModel(handle, repository) as T
            }
            modelClass.isAssignableFrom(IssuesViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return IssuesViewModel(handle, repository) as T
            }
            modelClass.isAssignableFrom(ViewIssueViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return ViewIssueViewModel(handle, repository) as T
            }
            modelClass.isAssignableFrom(PostCommentsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                return PostCommentsViewModel(handle, repository) as T
            }
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}