package io.github.vshnv.adapt.paging.adapter

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.vshnv.adapt.paging.dsl.collector.CollectingBindable
import java.util.Collections
import java.util.WeakHashMap

/**
 * An implementation of [PagedAdaptAdapter] that is lifecycle-aware.
 * It manages the lifecycle of individual [PagedAdaptViewHolder]s, ensuring that
 * resources are properly attached and detached as views are recycled or moved
 * in and out of the window.
 *
 * @param T The type of data items in the PagingData.
 * @param viewTypeMapper An optional function to map data items to view types.
 * @param defaultBinder An optional default binder for items without a specific view type.
 * @param viewBinders A map of view types to their corresponding binders.
 * @param diffCallback The [DiffUtil.ItemCallback] for efficient list updates.
 */
class LifecycleAwarePagedAdaptAdapter<T : Any>(
    private val viewTypeMapper: ((T, Int) -> Int)?,
    private val defaultBinder: CollectingBindable<T, *>?,
    private val viewBinders: MutableMap<Int, CollectingBindable<T, *>>,
    diffCallback: DiffUtil.ItemCallback<T>,
) : PagedAdaptAdapter<T, LifecycleAwarePagedAdaptViewHolder<T>>(diffCallback) {

    // Keep track of active ViewHolders to notify them when RecyclerView is detached
    // knownAffectedViewHolders might be redundant if LifecycleAwarePagedAdaptViewHolder manages its own lifecycle correctly.
    private val knownAffectedViewHolders =
        Collections.newSetFromMap(WeakHashMap<LifecycleAwarePagedAdaptViewHolder<T>, Boolean>())

    /**
     * Determines the view type for an item at a given position.
     * If a `viewTypeMapper` is provided, it's used; otherwise, the default behavior is applied.
     * @param position The position of the item.
     * @return The view type for the item.
     */
    override fun getItemViewType(position: Int): Int {
        // getItem(position) can return null for placeholders in Paging 3
        return viewTypeMapper?.let { mapper ->
            getItem(position)?.let { data ->
                mapper(data, position)
            } ?: super.getItemViewType(position) // Fallback for null data (placeholders)
        } ?: super.getItemViewType(position)
    }

    /**
     * Creates a new [PagedAdaptViewHolder] for a given view type.
     * It retrieves the appropriate binder (either specific or default) and
     * initializes a [LifecycleAwarePagedAdaptViewHolder].
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new [LifecycleAwarePagedAdaptViewHolder] instance.
     * @throws AssertionError if no binder is found for the given view type and no default binder is set.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): LifecycleAwarePagedAdaptViewHolder<T> {
        // Find the appropriate binder for the given view type, or use the default
        val binderItem: CollectingBindable<T, *> = viewBinders[viewType] ?: defaultBinder
        // Throw an error if no binder is found, as we cannot create the view
        ?: throw AssertionError("PagedAdapt found ViewType with no bound view creator or any default view creator, Cannot proceed!")

        // Create the view source using the binder's creator function
        val viewSource = binderItem.creator(parent)

        // Return a new LifecycleAwarePagedAdaptViewHolder
        return LifecycleAwarePagedAdaptViewHolder<T>(
            viewSource.view,
            attachLifecycle = { viewHolder, lifecycleOwner ->
                binderItem.lifecycleRenewAttachable?.attach?.invoke(
                    viewHolder,
                    getItem(viewHolder.bindingAdapterPosition)!!, // todo("// ?")
                    viewSource,
                    lifecycleOwner
                )
            }, bindRaw = { viewHolder, _, data ->
                // Lambda to bind data to the view
                val bindDataToView =
                    binderItem.bindDataToView ?: return@LifecycleAwarePagedAdaptViewHolder
                // Only bind if data is not null (i.e., not a placeholder)
                data?.let {
                    bindDataToView(viewHolder, it, viewSource)
                }
            })
    }

    /**
     * Binds data to the [PagedAdaptViewHolder].
     * @param holder The [PagedAdaptViewHolder] to bind data to.
     * @param position The position of the item in the adapter's data set.
     */
    override fun onBindViewHolder(holder: LifecycleAwarePagedAdaptViewHolder<T>, position: Int) {
        // Get the data item for the current position (can be null for placeholders)
        val data = getItem(position)
        // Bind the data to the holder
        holder.bind(position, data)
    }

    /**
     * Called when the RecyclerView is detached from the window.
     * This should trigger destruction of all active ViewHolder lifecycles.
     * This is typically handled by the RecyclerView itself or by the
     * LifecycleAwarePagedAdaptViewHolder's own lifecycle management when it's detached.
     */
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // If LifecycleAwarePagedAdaptViewHolder properly destroys its lifecycle
        // in onViewDetachedFromWindow, this might not need specific action here.
        // If you had any adapter-level observers for the RecyclerView's lifecycle,
        // you would remove them here.

        // Iterate through all known ViewHolders and notify them of detachment
        knownAffectedViewHolders.filterNotNull().forEach { viewHolder ->
            viewHolder.notifyDetached(recyclerView)
        }
    }

    /**
     * Called when a ViewHolder is attached to the window.
     * This is where the ViewHolder's lifecycle is set up and resumed.
     * @param holder The [PagedAdaptViewHolder] that was attached.
     */
    override fun onViewAttachedToWindow(holder: LifecycleAwarePagedAdaptViewHolder<T>) {
        super.onViewAttachedToWindow(holder)
        // Retrieve the lifecycle owner from the view tree
        val lifecycleOwner = ViewTreeLifecycleOwner.get(holder.itemView) ?: return
        // You still need to retrieve the parent lifecycle owner, as the ViewHolder's
        // lifecycle often needs to be in sync with its parent (e.g., the Fragment/Activity).
        // However, the ViewHolder itself acts as the LifecycleOwner for its observers.
//        holder.attachToLifecycle(lifecycleOwner)
        holder.handleLifecycleSetup(lifecycleOwner)
        // Set the highest state for the ViewHolder's lifecycle registry to RESUMED
        holder.lifecycleRegistry?.highestState = Lifecycle.State.RESUMED
        // Add the holder to the set of known active ViewHolders
        knownAffectedViewHolders.add(holder)
    }

    /**
     * Called when a ViewHolder is detached from the window.
     * This is where the ViewHolder's lifecycle state is set to CREATED or DESTROYED
     * depending on whether it's being recycled or permanently removed.
     * @param holder The [PagedAdaptViewHolder] that was detached.
     */
    override fun onViewDetachedFromWindow(holder: LifecycleAwarePagedAdaptViewHolder<T>) {
        // Set the highest state for the ViewHolder's lifecycle registry to CREATED
        holder.lifecycleRegistry?.highestState = Lifecycle.State.CREATED
        super.onViewDetachedFromWindow(holder)
    }
}