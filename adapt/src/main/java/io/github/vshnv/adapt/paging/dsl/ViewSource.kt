package io.github.vshnv.adapt.paging.dsl

import android.view.View

/**
 * Sealed interface representing different ways a view can be sourced for an item.
 * This abstracts whether the view is a direct [View] object or part of a ViewBinding.
 *
 * @param V The type of the view or view binding object.
 */
sealed interface ViewSource<V> {
    /**
     * The root [View] associated with this source.
     */
    val view: View

    /**
     * Represents a [ViewSource] where the view is a simple [View] object.
     * @param V The concrete type of the [View].
     * @param view The direct [View] instance.
     */
    data class SimpleViewSource<V: View>(override val view: V): ViewSource<V>

    /**
     * Represents a [ViewSource] where the view is obtained from a ViewBinding object.
     * @param V The type of the ViewBinding object.
     * @param binding The ViewBinding instance.
     * @param fetchViewRoot A lambda to extract the root [View] from the [binding] object.
     */
    data class BindingViewSource<V>(val binding: V, val fetchViewRoot: (V) -> View): ViewSource<V> {
        // The root view is lazily initialized from the binding
        override val view: View = fetchViewRoot(binding)
    }
}
