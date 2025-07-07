package io.github.vshnv.adapt.paging.dsl

import androidx.recyclerview.widget.RecyclerView

/**
 * Interface defining the scope for data binding within the PagedAdapt DSL.
 * This provides convenient access to the data, the view/binding object, and the ViewHolder
 * during the binding process.
 *
 * @param T The type of data item being bound.
 * @param V The type of the view or view binding object.
 */
interface BindScope<T, V> {
    /**
     * The adapter position of the item in the RecyclerView.
     */
    val index: Int

    /**
     * The data item being bound to the view.
     */
    val data: T

    /**
     * The view or view binding object associated with this item.
     */
    val binding: V

    /**
     * The [RecyclerView.ViewHolder] instance for this item.
     */
    val viewHolder: RecyclerView.ViewHolder
}