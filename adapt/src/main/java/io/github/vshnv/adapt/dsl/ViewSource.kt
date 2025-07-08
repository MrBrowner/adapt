package io.github.vshnv.adapt.dsl

import android.view.View

/**
 * Represents a source of a view or binding for a view type in the adapter.
 */
sealed interface ViewSource<V> {
    /**
     * The root view for this source.
     */
    val view: View

    /**
     * Simple view source wrapping a plain View.
     */
    data class SimpleViewSource<V: View>(override val view: V): ViewSource<V>
    /**
     * View source wrapping a binding object, with a function to fetch the root view.
     */
    data class BindingViewSource<V>(val binding: V, val fetchViewRoot: (V) -> View): ViewSource<V> {
        override val view: View = fetchViewRoot(binding)
    }
}
