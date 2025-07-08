package io.github.vshnv.adapt.dsl

import android.view.View
import androidx.viewbinding.ViewBinding

/**
 * Represents a source of a view or binding for a view type in the adapter.
 */
sealed class ViewSource<out T> {
    /**
     * The root view for this source.
     */
    abstract val view: View

    /**
     * Simple view source wrapping a plain View.
     */
    data class SimpleViewSource<out T : View>(override val view: T) : ViewSource<T>()

    /**
     * View source wrapping a binding object, with a function to fetch the root view.
     */
    data class BindingViewSource<out T : ViewBinding>(val binding: T) : ViewSource<T>() {
        override val view: View get() = binding.root
    }
}
