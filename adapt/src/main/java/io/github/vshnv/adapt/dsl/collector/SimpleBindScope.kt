package io.github.vshnv.adapt.dsl.collector

import androidx.recyclerview.widget.RecyclerView
import io.github.vshnv.adapt.dsl.BindScope

/**
 * SimpleBindScope provides binding context for a single item in the adapter.
 * @property data The data item being bound.
 * @property binding The view or binding object.
 * @property viewHolder The RecyclerView.ViewHolder for this item.
 * @property index The adapter position of this item. May be RecyclerView.NO_POSITION (-1) if not bound.
 */
data class SimpleBindScope<T, V>(
    override val data: T,
    override val binding: V,
    override val viewHolder: RecyclerView.ViewHolder
) : BindScope<T, V> {
    /**
     * The adapter position of this item. May be RecyclerView.NO_POSITION (-1) if not bound.
     */
    override val index: Int
        get() = viewHolder.bindingAdapterPosition
}