package io.github.vshnv.adapt.paging.dsl

/**
 * Interface representing an object that can bind data to a view.
 * This is part of the DSL for defining how data items are displayed.
 *
 * @param T The type of data item to be bound.
 * @param V The type of the view or view binding object.
 */
interface Bindable<T, V> {
    /**
     * Defines the binding logic for the view.
     *
     * @param bindView A lambda with [BindScope] as its receiver, providing access to
     * the data, view/binding, and ViewHolder. This is where you
     * implement the logic to update your view with the data.
     * @return A [LifecycleRenewAttachable] object, allowing you to further define
     * lifecycle-aware behavior for the binding.
     */
    fun bind(bindView: BindScope<T, V>.() -> Unit): LifecycleRenewAttachable<T, V>
}