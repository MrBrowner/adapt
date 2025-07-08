package io.github.vshnv.adapt.dsl

import android.view.ViewGroup
import android.widget.Filter

/**
 * Scope for configuring an Adapt adapter, including item comparison, view types, filtering, and view creation.
 */
interface AdaptScope<T : Any> {
    /**
     * Sets the function to check if two items are the same.
     */
    fun itemEquals(checkEquality: (data: T, otherData: T) -> Boolean)

    /**
     * Sets the function to check if two items' contents are the same.
     */
    fun contentEquals(checkContentEquality: (data: T, otherData: T) -> Boolean)

    /**
     * Defines how to map an item and its position to a view type integer.
     */
    fun defineViewTypes(mapToViewType: (data: T, position: Int) -> Int)

    /**
     * Sets the filter to be used for searching/filtering items.
     */
    fun filter(searchFilter: Filter?)

    /**
     * Creates a default view binder for the adapter.
     */
//    fun <V: Any> create(createView: (parent: ViewGroup) -> ViewSource<V>): Bindable<T, V>
    fun <V : Any> create(
        createView: (parent: ViewGroup) -> ViewSource<V>,
        viewType: Class<V>,
    ): Bindable<T, V>

    /**
     * Creates a view binder for a specific view type.
     */
//    fun <V : Any> create(viewType: Int, createView: (parent: ViewGroup) -> ViewSource<V>, ): Bindable<T, V>
    fun <V : Any> create(
        viewTypeInt: Int,
        createView: (parent: ViewGroup) -> ViewSource<V>,
        viewType: Class<V>,
    ): Bindable<T, V>
}