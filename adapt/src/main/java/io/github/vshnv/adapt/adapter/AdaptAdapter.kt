package io.github.vshnv.adapt.adapter

import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView

/**
 * Base adapter for Adapt, providing core list and filter operations.
 */
abstract class AdaptAdapter<T>: RecyclerView.Adapter<AdaptViewHolder<T>>(), Filterable {
    /**
     * The current list of items displayed by the adapter.
     */
    abstract val currentList: List<T>
    /**
     * The unfiltered list of items, if filtering is used.
     */
    abstract fun getUnfilteredList(): List<T>
    /**
     * Submit a new list of data to the adapter, suspending until complete.
     */
    abstract suspend fun submitDataSuspending(data: List<T>)
    /**
     * Submit a new list of data to the adapter, with an optional callback.
     */
    abstract fun submitData(data: List<T>, callback: () -> Unit = {})
    /**
     * Submit a new list of data from a filter operation, with an optional callback.
     */
    abstract fun submitDataFromFilter(data: List<T>, callback: () -> Unit = {})
}