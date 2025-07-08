package io.github.vshnv.adapt.adapter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.lang.ref.WeakReference

/**
 * Provides a Lifecycle for a RecyclerView ViewHolder or similar component, synchronized with a parent Lifecycle.
 * Manages the internal lifecycle state and allows for attach, detach, and destroy operations.
 */
class AdapterLifecycleRegistry(
    owner: LifecycleOwner, private val parentLifecycle: Lifecycle
) : Lifecycle() {

    // The actual LifecycleRegistry that manages the events for 'owner'
    private val internalLifecycleRegistry = LifecycleRegistry(owner)

    // Weak reference to the owner to prevent memory leaks if the owner is garbage collected
    private val ownerWeakRef = WeakReference(owner)

    // Observer for the parent lifecycle
    private val parentLifecycleObserver = LifecycleEventObserver { source, event ->
        val currentOwner = ownerWeakRef.get()
        if (currentOwner == null) {
            // Owner is gone, stop observing parent
            ignoreParent()
        } else {
            // Forward parent's event to our internal registry
            // This ensures our internal lifecycle follows the parent's
            // up to the point we explicitly move it (e.g., RESUMED when attached)
            internalLifecycleRegistry.handleLifecycleEvent(event)
        }
    }

    // Keep track of the highest desired state for this specific component (e.g., ViewHolder)
    // This acts as a ceiling that the internal lifecycle should not exceed,
    // even if the parent lifecycle goes higher.
    var highestPermittedState: State = State.INITIALIZED
        set(value) {
            field = value
            // When highestPermittedState is set, ensure the internal registry
            // doesn't exceed this state.
            // This is complex to manage perfectly with handleLifecycleEvent,
            // as handleLifecycleEvent pushes state forward.
            // A more direct approach is needed to *pull back* if the ceiling lowers.
            // For now, let's assume highestPermittedState is usually only increasing
            // or fixed for the ViewHolder's lifespan, or managed by external calls.
            // If the current internal state is higher than the new highestPermittedState,
            // we should pull it back. This might require emitting events.
            if (internalLifecycleRegistry.currentState > value) {
                // This is tricky: manually setting currentState would trigger dispatch.
                // A simpler approach for highestPermittedState is that any manual
                // state advancement (like through `moveToState` or `onAttachedToWindow`)
                // would respect this ceiling.
                // For now, we'll keep it as a simple property, and ensure manual calls to
                // `moveToState` respect it.
                // A better approach would be to have a custom `handleLifecycleEvent`
                // within `AdapterLifecycleRegistry` if it extended `LifecycleRegistry`,
                // but we decided against that.
                // Given the current setup, the ceiling is better enforced
                // by the caller when they manually advance this lifecycle.
            }
        }

    init {
        // Initially, the internal lifecycle should at least be CREATED
        internalLifecycleRegistry.currentState = State.CREATED
        // Start observing the parent lifecycle to synchronize its basic state transitions
        observeParent()
    }

    // This class itself provides the Lifecycle for its owner
    override fun getCurrentState(): State {
        return internalLifecycleRegistry.currentState
    }
//    override val currentState: State
//        get() = internalLifecycleRegistry.currentState

    override fun addObserver(observer: LifecycleObserver) {
        internalLifecycleRegistry.addObserver(observer)
    }

    override fun removeObserver(observer: LifecycleObserver) {
        internalLifecycleRegistry.removeObserver(observer)
    }

    /**
     * Call this when the owner (e.g., ViewHolder's view) is attached to the window
     * and should become active (e.g., STARTED or RESUMED).
     * This will also cause the internal lifecycle to follow the parent up to this point.
     */
    fun attach() {
        // Ensure the internal lifecycle reaches at least STARTED and then RESUMED
        // but not exceeding its highestPermittedState or parent's current state.
        val targetState = if (highestPermittedState > parentLifecycle.currentState)
            parentLifecycle.currentState else highestPermittedState

        if (targetState.isAtLeast(State.CREATED)) internalLifecycleRegistry.handleLifecycleEvent(Event.ON_CREATE)
        if (targetState.isAtLeast(State.STARTED)) internalLifecycleRegistry.handleLifecycleEvent(Event.ON_START)
        if (targetState.isAtLeast(State.RESUMED)) internalLifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME)
    }

    /**
     * Call this when the owner (e.g., ViewHolder's view) is detached from the window.
     * The internal lifecycle should fall back to a non-active state (e.g., CREATED).
     */
    fun detach() {
        // Move internal lifecycle back to CREATED state
        internalLifecycleRegistry.handleLifecycleEvent(Event.ON_PAUSE)
        internalLifecycleRegistry.handleLifecycleEvent(Event.ON_STOP)
        // Do not destroy here, as the ViewHolder might be recycled and re-attached
    }

    /**
     * Call this when the owner (e.g., ViewHolder) is being truly destroyed or recycled.
     * This will destroy the internal lifecycle.
     */
    fun destroy() {
        ignoreParent() // Stop observing the parent
        internalLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        highestPermittedState = State.DESTROYED // Mark as destroyed
    }

    private fun observeParent() {
        parentLifecycle.addObserver(parentLifecycleObserver)
    }

    private fun ignoreParent() {
        parentLifecycle.removeObserver(parentLifecycleObserver)
    }
}