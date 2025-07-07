package io.github.vshnv.adapt.paging.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Abstract base class for a ViewHolder in the PagedAdapt library.
 * All custom ViewHolders created via the DSL will implicitly extend this.
 *
 * @param T The type of data item this ViewHolder will bind.
 * @param view The root view of the ViewHolder's layout.
 */
abstract class PagedAdaptViewHolder<T>(view: View): RecyclerView.ViewHolder(view) {
    /**
     * Abstract method to be implemented by concrete ViewHolders for binding data.
     *
     * @param idx The adapter position of the item.
     * @param data The data item to bind. This can be `null` in Paging 3 for placeholders.
     */
    abstract fun bind(idx: Int, data: T?): Unit
}