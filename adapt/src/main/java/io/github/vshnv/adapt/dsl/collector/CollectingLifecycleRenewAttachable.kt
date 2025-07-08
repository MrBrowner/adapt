package io.github.vshnv.adapt.dsl.collector

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.github.vshnv.adapt.dsl.BindScope
import io.github.vshnv.adapt.dsl.LifecycleRenewAttachable
import io.github.vshnv.adapt.dsl.ViewSource

/**
 * CollectingLifecycleRenewAttachable collects lifecycle attachment logic for a view type.
 * @param createBindScope Function to create a SimpleBindScope for binding and lifecycle operations.
 */
class CollectingLifecycleRenewAttachable<T, V>(private val createBindScope: (RecyclerView.ViewHolder, T, ViewSource<*>) -> SimpleBindScope<T, V>) :
    LifecycleRenewAttachable<T, V> {
    /**
     * Function to attach lifecycle logic. Set by [withLifecycle].
     * If [withLifecycle] is not called, this will be a no-op and log a warning.
     */
    var attach: ((viewHolder: RecyclerView.ViewHolder, data: T, viewSource: ViewSource<*>, LifecycleOwner) -> Unit)? = { _, _, _, _ ->
        android.util.Log.w("CollectingLifecycleRenewAttachable", "withLifecycle() was not called; attach is a no-op.")
    }
    /**
     * Sets the lifecycle attachment logic for this view type.
     */
    override fun withLifecycle(attach: BindScope<T, V>.(LifecycleOwner) -> Unit) {
        this.attach = { viewHolder, data, viewSource, lifecycleOwner ->
            createBindScope(viewHolder, data, viewSource).attach(lifecycleOwner)
        }
    }

}