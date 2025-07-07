package io.github.vshnv.adapt.paging.lifecycle


import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.lang.ref.WeakReference

/**
 * A custom [LifecycleRegistry] designed for RecyclerView ViewHolders.
 * It observes a parent [Lifecycle] (e.g., from an Activity or Fragment) and
 * mirrors its state, but also respects a `highestState` property, ensuring
 * the ViewHolder's lifecycle doesn't advance beyond a desired state (e.g., CREATED when detached).
 *
 * @param owner The [LifecycleOwner] (typically a ViewHolder) that this registry belongs to.
 * @param parentLifecycle The [Lifecycle] of the parent (e.g., Activity or Fragment).
 */
class AdapterLifecycleRegistry(owner: LifecycleOwner, private val parentLifecycle: Lifecycle): LifecycleRegistry(owner) {
    // Weak reference to the owner to prevent memory leaks
    private val ownerWeakRef = WeakReference(owner)

    // Observer for the parent lifecycle
    private val parentLifecycleObserver = LifecycleEventObserver { _, _ ->
        // If the owner is no longer valid, stop observing the parent
        if (ownerWeakRef.get() == null) {
            ignoreParent()
        } else {
            // Otherwise, update the current state to match the parent's current state
            // This ensures the ViewHolder's lifecycle follows the parent's
            currentState = parentLifecycle.currentState
        }
    }

    // The highest state this lifecycle is allowed to reach.
    // This is crucial for controlling the ViewHolder's lifecycle when it's detached.
    var highestState = State.INITIALIZED
        set(value) {
            field = value
            // If the current state is already beyond INITIALIZED
            if (currentState > State.INITIALIZED) {
                // If parent's state is less than or equal to the new highestState,
                // set current state to parent's state
                if (parentLifecycle.currentState <= value) {
                    currentState = parentLifecycle.currentState
                } else if (currentState >= value) {
                    // If current state is already at or above the new highestState,
                    // set current state to the new highestState
                    currentState = value
                }
            }
        }

    init {
        // Initialize the state based on the parent's current state
        val currentParentState = parentLifecycle.currentState
        if (currentParentState > State.INITIALIZED) {
            highestState = parentLifecycle.currentState
            currentState = parentLifecycle.currentState
        }
        // Start observing the parent lifecycle
        observeParent()
    }

    /**
     * Adds the [parentLifecycleObserver] to the parent lifecycle.
     */
    private fun observeParent() {
        parentLifecycle.addObserver(parentLifecycleObserver)
    }

    /**
     * Removes the [parentLifecycleObserver] from the parent lifecycle.
     */
    private fun ignoreParent() {
        parentLifecycle.removeObserver(parentLifecycleObserver)
    }

    /**
     * Overrides [setCurrentState] to ensure the lifecycle does not exceed [highestState].
     * Also removes the parent observer if the state transitions to DESTROYED.
     *
     * @param nextState The desired next state for the lifecycle.
     */
    override fun setCurrentState(nextState: State) {
        // Determine the maximum allowed next state based on highestState
        val maxNextState = if (nextState > highestState)
            highestState else nextState

        // If the state is DESTROYED, stop observing the parent
        if (nextState == State.DESTROYED) {
            ignoreParent()
        }
        // Call the super method with the constrained state
        super.setCurrentState(maxNextState)
    }

    /**
     * Destroys the lifecycle registry. This is called when the ViewHolder is no longer needed.
     */
    fun destroy() {
        ignoreParent() // Stop observing parent
        highestState = State.DESTROYED // Set highest state to DESTROYED
    }
}
