package io.github.vshnv.adapt.paging.dsl.collector

import androidx.recyclerview.widget.RecyclerView
import io.github.vshnv.adapt.paging.dsl.BindScope

/**
 * A concrete implementation of [BindScope] that provides the necessary context
 * for data binding within the DSL.
 *
 * @param T The type of data item.
 * @param V The type of the view or view binding object.
 * @param data The data item being bound.
 * @param binding The view or view binding object.
 * @param viewHolder The [RecyclerView.ViewHolder] associated with this binding.
 */
data class SimpleBindScope<T, V>(
    override val data: T,
    override val binding: V,
    override val viewHolder: RecyclerView.ViewHolder
) : BindScope<T, V> {
    /**
     * The adapter position of the item.
     */
    override val index: Int
        get() = viewHolder.adapterPosition
}