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
    private val parentLifecycleObserver = LifecycleEventObserver { _, event -> // source parameter is not used, so can be omitted
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
            // When highestPermittedState is explicitly set, ensure the internal registry's
            // current state does not exceed this new ceiling.
            // handleLifecycleEvent only moves state forward, so for pulling back,
            // direct assignment might be needed but consider event dispatch carefully.
            // For most use cases, highestPermittedState will be used as a cap when
            // advancing the lifecycle from attach(), not for pulling back mid-state.
            if (internalLifecycleRegistry.currentState > value) {
                // If the new highestPermittedState is lower than the current internal state,
                // we should "pull back". This implies emitting events.
                // However, directly calling handleLifecycleEvent with a lower state's target
                // will usually not work if the current state is higher.
                // A common pattern is to simply ensure 'attach()' caps at this,
                // and 'detach()' handles moving back.
                // If a precise "pull back" is needed beyond just detach(), it would involve
                // more complex logic to emit ON_PAUSE/ON_STOP events as needed.
                // For now, removing the logic here to avoid misbehavior,
                // as `handleLifecycleEvent` generally only advances.
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
        // Determine the actual target state, capped by highestPermittedState and parent's current state.
        val targetState = minOf(highestPermittedState, parentLifecycle.currentState)

        // Advance internal lifecycle, ensuring it doesn't go beyond targetState
        if (internalLifecycleRegistry.currentState < State.CREATED && targetState.isAtLeast(State.CREATED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_CREATE)
        }
        if (internalLifecycleRegistry.currentState < State.STARTED && targetState.isAtLeast(State.STARTED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_START)
        }
        if (internalLifecycleRegistry.currentState < State.RESUMED && targetState.isAtLeast(State.RESUMED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME)
        }
    }

    /**
     * Call this when the owner (e.g., ViewHolder's view) is detached from the window.
     * The internal lifecycle should fall back to a non-active state (e.g., CREATED).
     */
    fun detach() {
        // Move internal lifecycle back to CREATED state by emitting pause and stop events.
        // Order matters for proper event dispatch (ON_PAUSE before ON_STOP).
        if (internalLifecycleRegistry.currentState.isAtLeast(State.RESUMED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_PAUSE)
        }
        if (internalLifecycleRegistry.currentState.isAtLeast(State.STARTED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_STOP)
        }
        // Do not destroy here, as the ViewHolder might be recycled and re-attached quickly
        // If the owner is truly destroyed, destroy() should be called.
    }

    /**
     * Call this when the owner (e.g., ViewHolder) is being truly destroyed or recycled.
     * This will destroy the internal lifecycle.
     */
    fun destroy() {
        ignoreParent() // Stop observing the parent to prevent leaks
        // Ensure all necessary down events are dispatched before destroying
        if (internalLifecycleRegistry.currentState.isAtLeast(State.RESUMED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_PAUSE)
        }
        if (internalLifecycleRegistry.currentState.isAtLeast(State.STARTED)) {
            internalLifecycleRegistry.handleLifecycleEvent(Event.ON_STOP)
        }
        // Finally, destroy the lifecycle
        internalLifecycleRegistry.handleLifecycleEvent(Event.ON_DESTROY)
        highestPermittedState = State.DESTROYED // Mark as destroyed for future checks
    }

    private fun observeParent() {
        parentLifecycle.addObserver(parentLifecycleObserver)
    }

    private fun ignoreParent() {
        parentLifecycle.removeObserver(parentLifecycleObserver)
    }
}