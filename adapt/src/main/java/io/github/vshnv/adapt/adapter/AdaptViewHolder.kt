package io.github.vshnv.adapt.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Base ViewHolder for Adapt, providing a bind method for data binding.
 */
abstract class AdaptViewHolder<T>(view: View): RecyclerView.ViewHolder(view) {
    /**
     * Bind the data item to the view at the given adapter position.
     */
    abstract fun bind(idx: Int, data: T)
}
