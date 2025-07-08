package io.github.vshnv.adapt.dsl.collector

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.vshnv.adapt.dsl.BindScope
import io.github.vshnv.adapt.dsl.Bindable
import io.github.vshnv.adapt.dsl.LifecycleRenewAttachable
import io.github.vshnv.adapt.dsl.ViewSource

/**
 * CollectingBindable is a Bindable implementation that collects binding and lifecycle logic for a view type.
 * @param creator Function to create a ViewSource for the view type.
 * @param expectedBindingType The Class object for the expected binding/view type V, required for runtime type checks.
 */
class CollectingBindable<T, V>(
    val creator: (parent: ViewGroup) -> ViewSource<V>,
    private val expectedBindingType: Class<V>, // Added for runtime type checking
) : Bindable<T, V> {

    var lifecycleRenewAttachable: CollectingLifecycleRenewAttachable<T, V>? = null

    /**
     * Function to bind data to the view. Set by [bind].
     * If [bind] is not called, this will be a no-op and log a warning.
     */
    var bindDataToView: ((viewHolder: ViewHolder, data: T, viewSource: ViewSource<*>) -> Unit)? =
        { _, _, _ ->
            Log.w("CollectingBindable", "bind() was not called; bindDataToView is a no-op.")
        }
        private set

    /**
     * Sets the binding logic for this view type.
     * @return a [LifecycleRenewAttachable] for attaching lifecycle logic.
     */
    override fun bind(bindView: BindScope<T, V>.() -> Unit): LifecycleRenewAttachable<T, V> {
        val createBindScope = { viewHolder: ViewHolder, data: T, viewSource: ViewSource<*> ->
            SimpleBindScope(
                data,
                resolveSourceParam(viewSource),
                viewHolder
            )
        }
        this.bindDataToView = { viewHolder: ViewHolder, data: T, viewSource: ViewSource<*> ->
            // The bindView lambda is an extension function on BindScope, so we need to call it on the created scope
            createBindScope(viewHolder, data, viewSource).bindView()
        }
        return CollectingLifecycleRenewAttachable<T, V>(createBindScope).also {
            lifecycleRenewAttachable = it
        }
    }

    /**
     * Resolves the correct type from a [ViewSource], with runtime type checks.
     * Uses the `expectedBindingType` from the class's constructor.
     * @throws IllegalStateException if the type does not match expected V.
     */
    private fun resolveSourceParam(item: ViewSource<*>): V {
        return when (item) {
            is ViewSource.BindingViewSource<*> -> {
                // Check if item.binding is an instance of the expectedBindingType
                if (!expectedBindingType.isInstance(item.binding)) {
                    throw IllegalStateException("BindingViewSource.binding is not of expected type: ${item.binding?.javaClass?.name}, expected: ${expectedBindingType.name}")
                }
                // Now that we've checked, we can cast
                @Suppress("UNCHECKED_CAST")
                item.binding as V
            }

            is ViewSource.SimpleViewSource<*> -> {
                // Check if item.view is an instance of the expectedBindingType
                if (!expectedBindingType.isInstance(item.view)) {
                    throw IllegalStateException("SimpleViewSource.view is not of expected type: ${item.view?.javaClass?.name}, expected: ${expectedBindingType.name}")
                }
                // Now that we've checked, we can cast
                @Suppress("UNCHECKED_CAST")
                item.view as V
            }
        }
    }

}