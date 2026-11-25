package dev.seeruk.bundle.leaders;

import net.minecraft.world.entity.decoration.Mannequin;

/**
 * The outcome of placing a leader mannequin.
 */
public sealed interface PlaceResult {
    // Success cases

    /**
     * A {@link PlaceResult} returned when the mannequin was placed.
     *
     * @param mannequin The mannequin that was placed.
     * @param seat      The seat it shows.
     */
    record Placed(Mannequin mannequin, Seat seat) implements PlaceResult {}

    // Failure cases
    sealed interface Failure extends PlaceResult {}

    /**
     * A {@link Failure} returned when there's no category with the seat's category ID.
     *
     * @param category The category ID that was given.
     */
    record UnknownCategory(String category) implements Failure {}

    /**
     * A {@link Failure} returned when the player isn't looking at a block within reach.
     */
    record NoBlockInSight() implements Failure {}

    /**
     * A {@link Failure} returned when there isn't room for a mannequin on the block.
     */
    record Obstructed() implements Failure {}
}
