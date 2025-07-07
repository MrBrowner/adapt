package io.github.vshnv.adapt.paging.dsl

import android.view.ViewGroup

/**
 * Interface defining the DSL (Domain Specific Language) for configuring a PagedAdaptAdapter.
 * This scope allows you to define how different types of paged data items are presented
 * in a RecyclerView.
 *
 * @param T The type of data items in the PagingData.
 */
interface PagedAdaptScope<T: Any> {
    /**
     * Defines a mapping function to determine the view type for each data item.
     * This is useful when you have multiple layouts or view holders for different
     * types of items in your paged list.
     *
     * @param mapToViewType A lambda that takes a data item [data] and its [position]
     * and returns an integer representing its view type.
     */
    fun defineViewTypes(mapToViewType: (data: T, position: Int) -> Int)

    /**
     * Defines a default view creator and binder for items that don't have a specific
     * view type defined. This serves as a fallback.
     *
     * @param V The type of the view or view binding object created.
     * @param createView A lambda that takes a [parent] ViewGroup and returns a [ViewSource]
     * (either a raw View or a ViewBinding object).
     * @return A [Bindable] object, which you can then use to define the binding logic
     * with the `bind` function.
     */
    fun <V: Any> create(createView: (parent: ViewGroup) -> ViewSource<V>): Bindable<T, V>

    /**
     * Defines a view creator and binder for a specific view type.
     * Use this when you have different layouts for different types of items.
     *
     * @param V The type of the view or view binding object created.
     * @param viewType An integer representing the unique view type.
     * @param createView A lambda that takes a [parent] ViewGroup and returns a [ViewSource]
     * (either a raw View or a ViewBinding object).
     * @return A [Bindable] object, which you can then use to define the binding logic
     * with the `bind` function.
     */
    fun <V: Any> create(viewType: Int, createView: (parent: ViewGroup) -> ViewSource<V>): Bindable<T, V>
}