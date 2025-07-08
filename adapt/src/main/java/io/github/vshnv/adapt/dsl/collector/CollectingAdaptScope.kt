package io.github.vshnv.adapt.dsl.collector

import android.view.ViewGroup
import android.widget.Filter
import io.github.vshnv.adapt.adapter.AdaptAdapter
import io.github.vshnv.adapt.adapter.LifecycleAwareAdaptAdapter
import io.github.vshnv.adapt.dsl.AdaptScope
import io.github.vshnv.adapt.dsl.Bindable
import io.github.vshnv.adapt.dsl.ViewSource

/**
 * Collects configuration for an Adapt adapter, including view types, binders, and filters.
 * Ensures all required properties are set before building the adapter.
 */
internal class CollectingAdaptScope<T : Any> : AdaptScope<T> {
    private var itemEquals: (T, T) -> Boolean = { a, b -> a == b }
    private var itemContentEquals: (T, T) -> Boolean = { a, b -> a == b }
    private var viewTypeMapper: ((T, Int) -> Int)? = null
    private var defaultBinder: CollectingBindable<T, *>? = null
    private val viewBinders: MutableMap<Int, CollectingBindable<T, *>> = mutableMapOf()
    private var searchFilterable: Filter? = null

    /**
     * Defines how to map an item and its position to a view type integer.
     */
    override fun defineViewTypes(mapToViewType: (T, Int) -> Int) {
        viewTypeMapper = mapToViewType
    }

    /**
     * Sets the function to check if two items are the same.
     */
    override fun itemEquals(checkEquality: (T, T) -> Boolean) {
        itemEquals = checkEquality
    }

    /**
     * Sets the function to check if two items' contents are the same.
     */
    override fun contentEquals(checkContentEquality: (T, T) -> Boolean) {
        itemContentEquals = checkContentEquality
    }

    /**
     * Sets the filter to be used for searching/filtering items.
     */
    override fun filter(searchFilter: Filter?) {
        searchFilterable = searchFilter
    }

    /**
     * Creates a default view binder for the adapter.
     */
    override fun <V : Any> create(
        createView: (ViewGroup) -> ViewSource<V>,
        viewType: Class<V>,
    ): Bindable<T, V> {
        val binder = CollectingBindable<T, V>(createView, viewType)
        defaultBinder = binder
        return binder
    }

    /**
     * Creates a view binder for a specific view type.
     */
    override fun <V : Any> create(
        viewTypeInt: Int,
        createView: (parent: ViewGroup) -> ViewSource<V>,
        viewType: Class<V>,
    ): Bindable<T, V> {
        val binder = CollectingBindable<T, V>(createView, viewType)
        viewBinders[viewTypeInt] = binder
        return binder
    }

    /**
     * Builds the [AdaptAdapter] using the collected configuration.
     * @throws IllegalStateException if required properties are missing.
     */
    internal fun buildAdapter(): AdaptAdapter<T> {
        val binder = requireNotNull(defaultBinder) {
            "No default binder defined. Call create() to define a default view binder before building the adapter."
        }
        // If multiple view types are used, viewTypeMapper must be set
        if (viewBinders.isNotEmpty()) {
            requireNotNull(viewTypeMapper) {
                "Multiple view types defined but no viewTypeMapper provided. Call defineViewTypes() to map items to view types."
            }
        }
        return LifecycleAwareAdaptAdapter<T>(
            viewTypeMapper,
            binder,
            viewBinders,
            itemEquals,
            itemContentEquals,
            searchFilterable
        )
    }
}