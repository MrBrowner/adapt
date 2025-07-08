package io.github.vshnv.adapt.dsl

import androidx.lifecycle.LifecycleOwner

/**
 * Allows attaching lifecycle logic to a view type in the adapter.
 */
interface LifecycleRenewAttachable<T, V> {
    /**
     * Sets the lifecycle attachment logic for this view type.
     */
    fun withLifecycle(attach: BindScope<T, V>.(LifecycleOwner) -> Unit)
}