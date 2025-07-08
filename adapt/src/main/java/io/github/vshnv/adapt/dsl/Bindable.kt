package io.github.vshnv.adapt.dsl

/**
 * Represents a bindable view type in the adapter, allowing binding logic to be defined.
 */
interface Bindable<T, V> {
    /**
     * Sets the binding logic for this view type.
     * @return a [LifecycleRenewAttachable] for attaching lifecycle logic.
     */
    fun bind(bindView: BindScope<T, V>.() -> Unit): LifecycleRenewAttachable<T, V>
}

