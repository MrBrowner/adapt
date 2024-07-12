package io.github.vshnv.adapt.adapter

import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView

abstract class AdaptAdapter<T>: RecyclerView.Adapter<AdaptViewHolder<T>>(), Filterable {
    abstract val currentList: List<T>
    abstract var searchFilterable: Filter?
    abstract fun getFullData(): List<T>

    abstract suspend fun submitDataSuspending(data: List<T>): Unit
    abstract fun submitData(data: List<T>, callback: () -> Unit = {}): Unit
    abstract fun submitDataFromFilter(data: List<T>, callback: () -> Unit = {}): Unit
}