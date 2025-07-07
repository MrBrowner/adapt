package io.github.vshnv.adapt.paging.dsl.collector

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.github.vshnv.adapt.paging.dsl.BindScope
import io.github.vshnv.adapt.paging.dsl.LifecycleRenewAttachable
import io.github.vshnv.adapt.paging.dsl.ViewSource

/**
 * Internal implementation of [LifecycleRenewAttachable] that collects the lifecycle
 * attachment logic defined in the DSL.
 *
 * @param T The type of data item.
 * @param V The type of the view or view binding object.
 * @param createBindScope A lambda to create a [SimpleBindScope] instance for the attachment.
 */
class CollectingLifecycleRenewAttachable<T, V>(
    private val createBindScope: (RecyclerView.ViewHolder, T, ViewSource<*>) -> SimpleBindScope<T, V>
) : LifecycleRenewAttachable<T, V> {

    // The actual lambda that performs the lifecycle attachment
    var attach: ((viewHolder: RecyclerView.ViewHolder, data: T, viewSource: ViewSource<*>, LifecycleOwner) -> Unit)? = null

    /**
     * Defines the lifecycle attachment logic.
     * @see LifecycleRenewAttachable.withLifecycle
     */
    override fun withLifecycle(attach: BindScope<T, V>.(LifecycleOwner) -> Unit) {
        // Store the attachment lambda, which creates a BindScope and then calls the user's attachment logic
        this.attach = { viewHolder, data, viewSource, lifecycleOwner ->
            createBindScope(viewHolder, data, viewSource).attach(lifecycleOwner)
        }
    }
}
