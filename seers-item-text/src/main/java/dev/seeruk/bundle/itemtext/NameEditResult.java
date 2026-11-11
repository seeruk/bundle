package dev.seeruk.bundle.itemtext;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * NameEditResult is a result type used to represent the outcome of attempting to edit the name of
 * an item.
 */
public sealed interface NameEditResult {
    // Success cases

    /**
     * A {@link NameEditResult} returned when the name was successfully edited. A new name being
     * accepted despite being identical to its current name also counts as successful.
     *
     * @param original The original name.
     * @param edited   The edited name.
     */
    record Edited(Optional<Component> original, Optional<Component> edited) implements NameEditResult {}

    // Failure cases
    sealed interface Failure extends NameEditResult {}

    /**
     * A {@link Failure} returned when a provided item name is blank.
     */
    record BlankName() implements Failure {}

    /**
     * A {@link Failure} returned when a provided item name's length is invalid.
     *
     * @param maxNameLength The maximum name length.
     * @param name          The name that has an invalid length.
     */
    record MaxLengthExceeded(int maxNameLength, Component name) implements Failure {}

    /**
     * A {@link Failure} returned when the item does not have a custom name.
     */
    record NoCustomName() implements Failure {}
}
