package io.github.vshnv.adapt

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class AdaptViewHolder<T>(view: View): RecyclerView.ViewHolder(view) {
    abstract fun bind(idx: Int, data: T): Unit
}
