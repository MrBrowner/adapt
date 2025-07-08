package io.github.vshnv.adapt.dsl

import androidx.recyclerview.widget.RecyclerView

/**
 * Provides binding context for a single item in the adapter.
 */
interface BindScope<T, V> {
    /**
     * The adapter position of this item. May be RecyclerView.NO_POSITION (-1) if not bound.
     */
    val index: Int
    /**
     * The data item being bound.
     */
    val data: T
    /**
     * The view or binding object.
     */
    val binding: V
    /**
     * The RecyclerView.ViewHolder for this item.
     */
    val viewHolder: RecyclerView.ViewHolder
}