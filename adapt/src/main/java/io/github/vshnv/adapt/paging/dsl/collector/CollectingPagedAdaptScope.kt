package io.github.vshnv.adapt.paging.dsl.collector

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.github.vshnv.adapt.paging.adapter.LifecycleAwarePagedAdaptAdapter
import io.github.vshnv.adapt.paging.adapter.PagedAdaptAdapter
import io.github.vshnv.adapt.paging.dsl.Bindable
import io.github.vshnv.adapt.paging.dsl.PagedAdaptScope
import io.github.vshnv.adapt.paging.dsl.ViewSource

/**
 * Internal implementation of [PagedAdaptScope] that collects all the configurations
 * provided via the DSL and uses them to build a [LifecycleAwarePagedAdaptAdapter].
 *
 * @param T The type of data items in the PagingData.
 * @param diffCallback The [DiffUtil.ItemCallback] provided during adapter creation.
 */
internal class CollectingPagedAdaptScope<T: Any>(
    private val diffCallback: DiffUtil.ItemCallback<T>
): PagedAdaptScope<T> {
    // Function to map data items to their respective view types
    private var viewTypeMapper: ((T, Int) -> Int)? = null
    // The default binder for items without a specific view type
    private var defaultBinder: CollectingBindable<T, *>? = null
    // A map storing binders for specific view types
    private val viewBinders: MutableMap<Int, CollectingBindable<T, *>> = mutableMapOf()

    /**
     * Sets the function to determine the view type for each data item.
     * @see PagedAdaptScope.defineViewTypes
     */
    override fun defineViewTypes(mapToViewType: (T, Int) -> Int) {
        viewTypeMapper = mapToViewType
    }

    /**
     * Creates a [Bindable] for a default view type. This binder will be used
     * if no specific view type binder is found for an item.
     * @see PagedAdaptScope.create
     */
    override fun <V: Any> create(createView: (parent: ViewGroup) -> ViewSource<V>): Bindable<T, V> {
        return CollectingBindable<T, V>(createView).apply {
            defaultBinder = this
        }
    }

    /**
     * Creates a [Bindable] for a specific view type.
     * @see PagedAdaptScope.create
     */
    override fun <V: Any> create(
        viewType: Int,
        createView: (parent: ViewGroup) -> ViewSource<V>
    ): Bindable<T, V> {
        return CollectingBindable<T, V>(createView).apply {
            viewBinders[viewType] = this
        }
    }

    /**
     * Builds and returns a [PagedAdaptAdapter] based on the collected configurations.
     * @return A new instance of [LifecycleAwarePagedAdaptAdapter].
     */
    internal fun buildAdapter(): PagedAdaptAdapter<T> {
        return LifecycleAwarePagedAdaptAdapter<T>(
            viewTypeMapper,
            defaultBinder,
            viewBinders,
            diffCallback
        )
    }
}