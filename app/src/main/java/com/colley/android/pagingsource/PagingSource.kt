package com.colley.android.pagingsource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import kotlinx.coroutines.tasks.await
import android.annotation.SuppressLint
import java.util.ArrayList
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.snapshot.PathIndex
import java.util.concurrent.ExecutionException


class PagingSource(private val mQuery: Query) : PagingSource<Any, DataSnapshot>() {

    @SuppressLint("RestrictedApi")
    fun getChildValue(snapshot: DataSnapshot, index: PathIndex):Any? {
        val keypath = index.queryDefinition
        val data=snapshot.child(keypath)
        if(!data.exists()) return null
        return data.value
    }
    fun Query.startAt_childvalue(startvalue:Any?,keyvalue:String?):Query {
        return when(startvalue) {
            is String -> startAt(startvalue,keyvalue)
            is Boolean -> startAt(startvalue,keyvalue)
            is Double -> startAt(startvalue,keyvalue)
            is Long -> startAt(startvalue.toDouble(),keyvalue)
            else -> this
        }
    }

//    reason of SuppressLint: DatabaseError.fromStatus() is not meant to be public

    @SuppressLint("RestrictedApi")
    override suspend fun load(params: LoadParams<Any>): LoadResult<Any, DataSnapshot> {
        //change mQuery.startAt at value  if child index

        //if not null then what we have here is orderByChild query
        val querychildpathindex:PathIndex? = mQuery.spec.index as? PathIndex
        val pkey = params.key as Pair<Any?,String>?

        val task: Task<DataSnapshot> =

            if (params.key == null) {
                mQuery.limitToFirst(params.loadSize).get()
            } else {
                if (querychildpathindex != null)  //orderByChild query mode
                    mQuery.startAt_childvalue(pkey?.first, pkey?.second)
                        .limitToFirst(params.loadSize + 1).get()
                else
                    mQuery.startAt(null,pkey?.second).limitToFirst(params.loadSize + 1)
                        .get()
            }
        try {
            val dataSnapshot = task.await()
            if (dataSnapshot.exists()) {
                //Make List of DataSnapshot
                val data: MutableList<DataSnapshot> = ArrayList()
                var lastKey: Pair<Any?,String>? = null
                if (params.key == null) {
                    for (snapshot in dataSnapshot.children) {
                        data.add(snapshot)
                    }
                } else {
                    val iterator: Iterator<DataSnapshot> = dataSnapshot.children.iterator()

                    //Skip First Item that corresponds to lastKey read in previous batch
                    if (iterator.hasNext()) {
                        iterator.next()
                    }
                    while (iterator.hasNext()) {
                        val snapshot = iterator.next()
                        data.add(snapshot)
                    }
                }

                //Detect End of Data
                if (!data.isEmpty()) {
                    //Get Last Key
                    val lastkey_c = getLastPageChildKey(data,querychildpathindex)
                    val lastkey_k = getLastPageKey(data)
                    lastKey = if (lastkey_c == null && lastkey_k == null)
                        null
                    else
                        if (lastkey_k == null) Pair(lastkey_c, "") else Pair(lastkey_c, lastkey_k)
                }
                return toLoadResult(data, lastKey)
            } else {
                val details = DETAILS_DATABASE_NOT_FOUND + mQuery.toString()
                throw DatabaseError.fromStatus(
                    STATUS_DATABASE_NOT_FOUND,
                    MESSAGE_DATABASE_NOT_FOUND,
                    details
                ).toException()
            }
        } catch (e: ExecutionException) {
            return LoadResult.Error<Any, DataSnapshot>(e)
        } catch (e: Exception) {
            return LoadResult.Page(
                arrayListOf(),
                null,
                null,
                LoadResult.Page.COUNT_UNDEFINED,
                LoadResult.Page.COUNT_UNDEFINED
            )
        }
    }


    private fun toLoadResult(
        snapshots: List<DataSnapshot>,
        nextPage: Pair<Any?,String>?
    ): LoadResult<Any, DataSnapshot> {
        return LoadResult.Page(
            snapshots,
            null,  // Only paging forward.
            nextPage,
            LoadResult.Page.COUNT_UNDEFINED,
            LoadResult.Page.COUNT_UNDEFINED
        )
    }
    private fun getLastPageChildKey(data: List<DataSnapshot>,index: PathIndex?): Any? {
        if(index==null) return null
        return if (data.isEmpty()) {
            null
        } else {
            getChildValue(data[data.size - 1],index)
        }
    }

    private fun getLastPageKey(data: List<DataSnapshot>): String? {
        return if (data.isEmpty()) {
            null
        } else {
            data[data.size - 1].key
        }
    }

    override fun getRefreshKey(state: PagingState<Any, DataSnapshot>): Pair<Any?, String>? {
        return null
    }

    companion object {
        private const val STATUS_DATABASE_NOT_FOUND = "DATA_NOT_FOUND"
        private const val MESSAGE_DATABASE_NOT_FOUND = "Data not found at given child path!"
        private const val DETAILS_DATABASE_NOT_FOUND = "No data was returned for the given query: "
    }
}