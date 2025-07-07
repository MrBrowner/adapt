package io.github.vshnv.adapt.paging.adapter

import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil

/**
 * Abstract base class for a PagedAdaptAdapter, extending AndroidX's [PagingDataAdapter].
 * This class provides the fundamental structure for an adapter that handles paged data
 * and integrates with the PagedAdapt DSL.
 *
 * @param T The type of data items in the PagingData.
 * @param VH The type of [PagedAdaptViewHolder] used by this adapter.
 */
abstract class PagedAdaptAdapter<T: Any, VH: PagedAdaptViewHolder<T>>(
    diffCallback: DiffUtil.ItemCallback<T>
): PagingDataAdapter<T, VH>(diffCallback) {
    // No additional abstract methods needed here, as PagingDataAdapter handles data submission.
    // The filtering mechanism is handled by PagingSource, not directly by the adapter.
}