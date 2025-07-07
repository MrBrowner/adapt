package io.github.vshnv.adapt.paging.dsl.collector

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.vshnv.adapt.paging.dsl.BindScope
import io.github.vshnv.adapt.paging.dsl.Bindable
import io.github.vshnv.adapt.paging.dsl.LifecycleRenewAttachable
import io.github.vshnv.adapt.paging.dsl.ViewSource

/**
 * Internal implementation of [Bindable] that collects the view creation and binding logic.
 *
 * @param T The type of data item.
 * @param V The type of the view or view binding object.
 * @param creator A lambda function that creates the [ViewSource] for the item.
 */
class CollectingBindable<T, V>(val creator: (parent: ViewGroup) -> ViewSource<V>): Bindable<T, V> {

    // Reference to the lifecycle renewable attachable, if defined
    var lifecycleRenewAttachable: CollectingLifecycleRenewAttachable<T, V>? = null
    // The actual lambda that performs data binding to the view
    var bindDataToView: ((viewHolder: ViewHolder, data: T, viewSource: ViewSource<*>) -> Unit)? = null
        private set

    /**
     * Defines the binding logic for the view.
     * @see Bindable.bind
     */
    override fun bind(bindView: BindScope<T, V>.() -> Unit): LifecycleRenewAttachable<T, V> {
        // Create a lambda that constructs a SimpleBindScope for the binding operation
        val createBindScope = { viewHolder: ViewHolder, data: T, viewSource: ViewSource<*> ->
            SimpleBindScope(
                data,
                resolveSourceParam(viewSource), // Resolve the actual view/binding object
                viewHolder
            )
        }
        // Store the binding lambda that will be called to apply the DSL binding logic
        this.bindDataToView = { viewHolder: ViewHolder, data: T, viewSource: ViewSource<*> ->
            createBindScope(viewHolder, data, viewSource).bindView()
        }
        // Return a CollectingLifecycleRenewAttachable to allow defining lifecycle-aware behavior
        return CollectingLifecycleRenewAttachable<T, V>(createBindScope).also {
            lifecycleRenewAttachable = it
        }
    }

    /**
     * Resolves the actual view or binding object from a [ViewSource].
     * Performs an unchecked cast, relying on the DSL's type inference and correct usage.
     *
     * @param item The [ViewSource] to resolve.
     * @return The actual view or binding object.
     */
    private fun resolveSourceParam(item: ViewSource<*>): V {
        @Suppress("UNCHECKED_CAST") // This cast is safe by design of the DSL
        return when (item) {
            is ViewSource.BindingViewSource<*> -> item.binding as V
            is ViewSource.SimpleViewSource<*> -> item.view as V
        }
    }
}