package io.github.vshnv.adapt.extensions

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView

/**
 * Finds the [LifecycleOwner] associated with a [View] in its view tree.
 * This is typically the Activity or Fragment that hosts the view.
 *
 * @return The [LifecycleOwner] if found, otherwise `null`.
 */
internal fun View.findViewTreeLifecycleOwner(): LifecycleOwner? {
    return ViewTreeLifecycleOwner.get(this)
}

/**
 * Recursively finds the closest [RecyclerView] parent of this [View].
 *
 * @return The closest [RecyclerView] parent if found, otherwise `null`.
 */
internal fun View.findClosestRecyclerView(): RecyclerView? {
    return when (this) {
        is RecyclerView -> this // If the view itself is a RecyclerView, return it
        else -> when (val parent = parent) {
            is View -> parent.findClosestRecyclerView() // If parent is a View, recurse
            else -> null // No more parents or parent is not a View
        }
    }
}