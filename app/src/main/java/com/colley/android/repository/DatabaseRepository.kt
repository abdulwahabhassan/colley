package com.colley.android.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.colley.android.pagingsource.PagingSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import kotlinx.coroutines.flow.Flow

class DatabaseRepository {

    fun getDataStream(query: Query): Flow<PagingData<DataSnapshot>> {
        return Pager(
            config = PagingConfig(
                pageSize = DATABASE_PAGE_SIZE,
                prefetchDistance = DATABASE_PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PagingSource(query) }
        ).flow
    }

    companion object {
        const val DATABASE_PAGE_SIZE = 10
        const val DATABASE_PREFETCH_DISTANCE = 5
    }
}