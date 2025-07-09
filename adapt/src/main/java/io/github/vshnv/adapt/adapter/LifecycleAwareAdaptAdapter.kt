package io.github.vshnv.adapt.adapter

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.vshnv.adapt.dsl.collector.CollectingBindable
import kotlin.coroutines.suspendCoroutine

// Ensure AdapterLifecycleRegistry is correctly defined as provided in the previous turn
// and that it now has attach(), detach(), and destroy() methods.
// Also ensure your AdaptViewHolder is open so LifecycleAwareAdaptViewHolder can extend it.

class LifecycleAwareAdaptAdapter<T : Any>(
    private val viewTypeMapper: ((T, Int) -> Int)?,
    private val defaultBinder: CollectingBindable<T, *>?,
    private val viewBinders: MutableMap<Int, CollectingBindable<T, *>>,
    private val itemEquals: (T, T) -> Boolean,
    private val itemContentEquals: (T, T) -> Boolean,
    private var searchFilter: Filter?,
) : AdaptAdapter<T>() {

    private val diffCallback: DiffUtil.ItemCallback<T> = object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
            return itemEquals(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
            return itemContentEquals(oldItem, newItem)
        }
    }
    private val mDiffer: AsyncListDiffer<T> = AsyncListDiffer(this, diffCallback)
    override val currentList: List<T>
        get() = mDiffer.currentList
    private var unFilteredList: MutableList<T> = mutableListOf()

    override fun getFilter(): Filter = requireNotNull(searchFilter) {
        "Filterable.Filter of $this accessed before assigning"
    }

    override fun getUnfilteredList(): List<T> = unFilteredList

    override fun getItemViewType(position: Int): Int {
        return viewTypeMapper?.let {
            it(getItem(position), position)
        } ?: super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdaptViewHolder<T> {
        val binderItem: CollectingBindable<T, *> = viewBinders[viewType] ?: defaultBinder
        ?: throw AssertionError("Adapt found ViewType with no bound view creator or any default view creator, Cannot proceed!")
        val viewSource = binderItem.creator(parent)

        return LifecycleAwareAdaptViewHolder<T>(
            viewSource.view,
            // Pass the binder's attach lambda to the ViewHolder
            attachLifecycle = { viewHolder, lifecycleOwnerForAttach ->
                binderItem.lifecycleRenewAttachable?.attach?.invoke(
                    viewHolder,
                    currentList[viewHolder.bindingAdapterPosition],
                    viewSource,
                    lifecycleOwnerForAttach // This is the AdapterLifecycleRegistry instance itself
                )
            },
            // Pass the binder's bind lambda to the ViewHolder
            bindRaw = { viewHolder, _, data ->
                val bindDataToView =
                    binderItem.bindDataToView ?: return@LifecycleAwareAdaptViewHolder
                bindDataToView(viewHolder, data, viewSource)
            }
        )
    }

    override fun getItemCount(): Int {
        return mDiffer.currentList.size
    }

    override fun onBindViewHolder(holder: AdaptViewHolder<T>, position: Int) {
        // Ensure the holder is the correct type
        (holder as? LifecycleAwareAdaptViewHolder<T>)?.bind(position, getItem(position))
    }

    private fun getItem(position: Int): T {
        return mDiffer.currentList[position]
    }

    override suspend fun submitDataSuspending(data: List<T>) {
        unFilteredList = data.toMutableList()
        suspendCoroutine<Unit> { continuation ->
            mDiffer.submitList(data) {
                continuation.resumeWith(Result.success(Unit))
            }
        }
    }

    override fun submitData(data: List<T>, callback: () -> Unit) {
        unFilteredList = data.toMutableList()
        mDiffer.submitList(data, callback)
    }

    override fun submitDataFromFilter(data: List<T>, callback: () -> Unit) {
        mDiffer.submitList(data, callback)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // onViewRecycled and onViewDetachedFromWindow handle individual holder lifecycles.
        // If there were any adapter-level observers for RecyclerView's lifecycle, they would be removed here.
    }

    override fun onViewAttachedToWindow(holder: AdaptViewHolder<T>) {
        super.onViewAttachedToWindow(holder)

        val instanceOfAdaptViewHolder = LifecycleAwareAdaptViewHolder::class.java.isInstance(holder)
        Log.e("TAG", "onViewAttachedToWindow: instanceOfAdaptViewHolder = $instanceOfAdaptViewHolder", )
        if (instanceOfAdaptViewHolder){
        }

        val lifecycleAwareHolder = holder as LifecycleAwareAdaptViewHolder<T>
        val parentLifecycleOwner = ViewTreeLifecycleOwner.get(lifecycleAwareHolder.itemView) ?: return

        // Initialize and attach the AdapterLifecycleRegistry
        lifecycleAwareHolder.handleLifecycleSetup(parentLifecycleOwner)
        lifecycleAwareHolder.lifecycleRegistry?.attach() // Call attach() to move to RESUMED
    }

    override fun onViewDetachedFromWindow(holder: AdaptViewHolder<T>) {
        super.onViewDetachedFromWindow(holder)
        val lifecycleAwareHolder = holder as LifecycleAwareAdaptViewHolder<T>

        // Detach the AdapterLifecycleRegistry
        lifecycleAwareHolder.lifecycleRegistry?.detach() // Call detach() to move to CREATED/STOPPED
    }

    // Crucial for proper lifecycle cleanup when ViewHolder is truly recycled
    override fun onViewRecycled(holder: AdaptViewHolder<T>) {
        super.onViewRecycled(holder)
        val lifecycleAwareHolder = holder as LifecycleAwareAdaptViewHolder<T>

        // Destroy the AdapterLifecycleRegistry
        lifecycleAwareHolder.lifecycleRegistry?.destroy() // Call destroy() to clean up
        lifecycleAwareHolder.lifecycleRegistry = null // Clear reference
    }


    class LifecycleAwareAdaptViewHolder<T>(
        view: View,
        // The attachLifecycle lambda now expects the AdapterLifecycleRegistry (which is a LifecycleOwner)
        private val attachLifecycle: (ViewHolder, LifecycleOwner) -> Unit,
        private val bindRaw: (LifecycleAwareAdaptViewHolder<T>, Int, T) -> Unit,
    ) : AdaptViewHolder<T>(view), LifecycleOwner {

        private var lastData: T? = null // For data-driven lifecycle renewal
        private var lastParentLifecycleOwner: LifecycleOwner? = null // The Activity/Fragment's LifecycleOwner
        var lifecycleRegistry: AdapterLifecycleRegistry? = null

        // LifecycleOwner implementation
        override fun getLifecycle(): Lifecycle =
            requireNotNull(lifecycleRegistry) { "Lifecycle of $this accessed before it was initialized." }

        override fun bind(idx: Int, data: T) {
            // Only renew if data is actually different and a lifecycleOwner exists.
            // This prevents unnecessary lifecycle renewals on every re-bind with same data.
            if (lastData != data && lastParentLifecycleOwner != null) {
                // If data changes, renew the registry
                renewLifecycleRegistry(lastParentLifecycleOwner!!)
            }
            bindRaw(this, idx, data)
            lastData = data
        }

        // Sets up or renews the AdapterLifecycleRegistry, linking it to the parent LifecycleOwner
        fun handleLifecycleSetup(parentLifecycleOwner: LifecycleOwner) {
            // Only setup/renew if the parent LifecycleOwner has changed, or if registry is null
            if (lastParentLifecycleOwner == parentLifecycleOwner && lifecycleRegistry != null) {
                return // Already set up for this parent
            }
            lastParentLifecycleOwner = parentLifecycleOwner
            renewLifecycleRegistry(parentLifecycleOwner) // Create/renew registry
        }

        // Renews the AdapterLifecycleRegistry, effectively restarting the ViewHolder's lifecycle
        private fun renewLifecycleRegistry(parentLifecycleOwner: LifecycleOwner) {
            // Destroy the old registry first if it exists
            lifecycleRegistry?.destroy()
            // Create a new AdapterLifecycleRegistry, passing itself (this ViewHolder) as the owner
            // and the parent's lifecycle for synchronization.
            lifecycleRegistry = AdapterLifecycleRegistry(this, parentLifecycleOwner.lifecycle)
            // Call the attachLifecycle lambda provided by the adapter,
            // passing this ViewHolder (which is the LifecycleOwner)
            attachLifecycle(this, this)
        }

        // findClosestRecyclerView is typically not needed for lifecycle management.
        // ViewTreeLifecycleOwner.get(itemView) is the standard for getting the correct LifecycleOwner.
        // This method can likely be removed.
        private fun View.findClosestRecyclerView(): RecyclerView? {
            return when (this) {
                is RecyclerView -> this
                else -> when (val parent = parent) {
                    is View -> parent.findClosestRecyclerView()
                    else -> null
                }
            }
        }
    }

}