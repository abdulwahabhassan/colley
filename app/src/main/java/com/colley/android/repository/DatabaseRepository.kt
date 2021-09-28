package com.colley.android.repository

import androidx.paging.*
import com.colley.android.pagingsource.PagingSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

class DatabaseRepository {



    fun getDataStream(query: Query): Flow<PagingData<DataSnapshot>> {
        return Pager(
            config = PagingConfig(
                pageSize = DATABASE_PAGE_SIZE,
                prefetchDistance = DATABASE_PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = InvalidatingPagingSourceFactory{ PagingSource(query) }
        ).flow
    }


    companion object {
        const val DATABASE_PAGE_SIZE = 20
        const val DATABASE_PREFETCH_DISTANCE = 10
    }
}