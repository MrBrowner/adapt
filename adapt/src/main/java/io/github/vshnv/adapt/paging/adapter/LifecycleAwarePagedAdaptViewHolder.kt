package io.github.vshnv.adapt.paging.adapter

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.vshnv.adapt.extensions.findClosestRecyclerView
import io.github.vshnv.adapt.paging.lifecycle.AdapterLifecycleRegistry

/**
 * A lifecycle-aware implementation of [PagedAdaptViewHolder].
 * This ViewHolder itself acts as a [LifecycleOwner], allowing other lifecycle-aware
 * components to observe its lifecycle. It manages an internal [AdapterLifecycleRegistry]
 * that mirrors the parent's lifecycle while also respecting its own attachment state.
 *
 * @param T The type of data item this ViewHolder will bind.
 * @param view The root view of the ViewHolder's layout.
 * @param attachLifecycle A lambda to attach lifecycle observers when the ViewHolder's
 * lifecycle is renewed.
 * @param bindRaw A lambda to perform the raw data binding logic.
 */
class LifecycleAwarePagedAdaptViewHolder<T>(
    view: View,
    private val attachLifecycle: (ViewHolder, LifecycleOwner) -> Unit,
    private val bindRaw: (LifecycleAwarePagedAdaptViewHolder<T>, Int, T?) -> Unit
) : PagedAdaptViewHolder<T>(view), LifecycleOwner {

    private var lastData: T? = null // Stores the last bound data for comparison
    private var lastAttachedRecyclerView: RecyclerView? = null // Stores the RecyclerView it's attached to
    private var lastLifecycleOwner: LifecycleOwner? = null // Stores the parent LifecycleOwner

    // The internal LifecycleRegistry for this ViewHolder
    var lifecycleRegistry: AdapterLifecycleRegistry? = null
        private set

    /**
     * Provides the Lifecycle for this ViewHolder. Throws an error if accessed before binding.
     */
    override fun getLifecycle(): Lifecycle = requireNotNull(lifecycleRegistry) {
        "LifeCycle of $this accessed before attempting bind"
    }

    /**
     * Binds data to the ViewHolder. If the data has changed and a lifecycle owner is present,
     * the internal lifecycle registry is renewed.
     *
     * @param idx The adapter position of the item.
     * @param data The data item to bind. Can be `null` for Paging 3 placeholders.
     */
    override fun bind(idx: Int, data: T?) {
        // If data has changed and we have a lifecycle owner, renew the lifecycle
        if (lastData != data && lastLifecycleOwner != null) {
            renewLifecycleRegistry(lastLifecycleOwner!!)
        }
        // Perform the raw data binding
        bindRaw(this, idx, data)
        lastData = data // Update last bound data
    }

    /**
     * Handles the initial setup of the ViewHolder's lifecycle.
     * This is typically called when the ViewHolder is attached to the window.
     *
     * @param lifecycleOwner The parent [LifecycleOwner] (e.g., Activity, Fragment).
     */
    fun handleLifecycleSetup(lifecycleOwner: LifecycleOwner) {
        // If already set up with the same lifecycle owner, do nothing
        if (lastLifecycleOwner == lifecycleOwner) {
            return
        }
        lastLifecycleOwner = lifecycleOwner // Store the parent lifecycle owner
        lastAttachedRecyclerView = itemView.findClosestRecyclerView() // Find the closest RecyclerView parent
        renewLifecycleRegistry(lifecycleOwner) // Renew the internal lifecycle registry
    }

    /**
     * Notifies the ViewHolder that it has been detached from a RecyclerView.
     * This destroys the internal lifecycle registry and clears associated references.
     *
     * @param recyclerView The RecyclerView from which this ViewHolder was detached.
     */
    fun notifyDetached(recyclerView: RecyclerView) {
        // Only proceed if detached from the currently attached RecyclerView
        if (lastAttachedRecyclerView != recyclerView) {
            return
        }
        lifecycleRegistry?.destroy() // Destroy the lifecycle registry
        lifecycleRegistry = null // Clear the reference
        lastLifecycleOwner = null // Clear the parent lifecycle owner reference
        lastAttachedRecyclerView = null // Clear the RecyclerView reference
    }

    /**
     * Renews the internal [AdapterLifecycleRegistry]. This involves destroying the old one
     * (if it exists) and creating a new one, then attaching lifecycle observers.
     *
     * @param lifecycleOwner The parent [LifecycleOwner] to observe.
     */
    private fun renewLifecycleRegistry(lifecycleOwner: LifecycleOwner) {
        lifecycleRegistry?.destroy() // Destroy existing registry
        lifecycleRegistry = AdapterLifecycleRegistry(this, lifecycleOwner.lifecycle) // Create new registry
        attachLifecycle(this, this) // Attach lifecycle observers
    }
}