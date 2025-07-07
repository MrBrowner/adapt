package io.github.vshnv.adapt.paging.dsl

import androidx.recyclerview.widget.DiffUtil
import io.github.vshnv.adapt.paging.adapter.PagedAdaptAdapter
import io.github.vshnv.adapt.paging.dsl.collector.CollectingPagedAdaptScope

/**
 * Creates a new [PagedAdaptAdapter] using a declarative DSL for Android Paging 3.
 * This function is the entry point for defining how your paged data items are displayed
 * in a RecyclerView, including view types and data binding logic.
 *
 * @param T The type of data items in the PagingData.
 * @param diffCallback The [DiffUtil.ItemCallback] used by PagingDataAdapter to calculate
 * differences between lists. This is crucial for efficient updates.
 * @param setup A lambda with [PagedAdaptScope] as its receiver, allowing you to define
 * view types and binding rules for your paged items.
 * @return A configured [PagedAdaptAdapter] ready to be used with a RecyclerView.
 */
fun <T: Any> pagedAdapt(
    diffCallback: DiffUtil.ItemCallback<T>,
    setup: PagedAdaptScope<T>.() -> Unit
): PagedAdaptAdapter<T> {
    // Create a scope to collect all the adapter configurations
    val pagedAdaptScope = CollectingPagedAdaptScope<T>(diffCallback)
    // Apply the user's setup lambda to configure the adapter
    pagedAdaptScope.setup()
    // Build and return the PagedAdaptAdapter
    return pagedAdaptScope.buildAdapter()
}
