package dev.seeruk.bundle.leaders;

/**
 * The outcome of removing a leader mannequin.
 */
public sealed interface RemoveResult {
    // Success cases

    /**
     * A {@link RemoveResult} returned when the mannequin was removed.
     *
     * @param seat The seat it showed.
     */
    record Removed(Seat seat) implements RemoveResult {}

    // Failure cases
    sealed interface Failure extends RemoveResult {}

    /**
     * A {@link Failure} returned when the player isn't looking at a leader mannequin within reach.
     */
    record NotLookingAtLeaderMannequin() implements Failure {}
}
