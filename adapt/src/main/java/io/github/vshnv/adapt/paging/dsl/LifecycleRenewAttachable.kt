package io.github.vshnv.adapt.paging.dsl

import androidx.lifecycle.LifecycleOwner

/**
 * Interface for objects that allow defining lifecycle-aware behavior for a binding.
 * This is used to attach observers that react to the ViewHolder's lifecycle.
 *
 * @param T The type of data item.
 * @param V The type of the view or view binding object.
 */
interface LifecycleRenewAttachable<T, V> {
    /**
     * Defines a lambda that will be executed when the ViewHolder's lifecycle is attached
     * or renewed. This allows you to set up observers or other lifecycle-dependent operations.
     *
     * @param attach A lambda with [BindScope] as its receiver, also providing the
     * [LifecycleOwner] of the ViewHolder.
     */
    fun withLifecycle(attach: BindScope<T, V>.(LifecycleOwner) -> Unit)
}