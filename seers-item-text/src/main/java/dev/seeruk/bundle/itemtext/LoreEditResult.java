package dev.seeruk.bundle.itemtext;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * LoreEditResult is a result type used to represent the outcome of attempting to edit the lore of
 * an item.
 */
public sealed interface LoreEditResult {
    // Success cases
    /**
     * A {@link LoreEditResult} returned when the lore was successfully edited.
     *
     * @param original The original lore.
     * @param edited   The edited lore.
     */
    record Edited(List<Component> original, List<Component> edited) implements LoreEditResult {
        public Edited {
            // Ensure that the resulting lists are immutable
            original = List.copyOf(original);
            edited = List.copyOf(edited);
        }
    }

    // Failure cases
    sealed interface Failure extends LoreEditResult {}

    /**
     * A {@link Failure} returned when an individual line is longer than the maximum line length.
     *
     * @param maxLineLength The maximum line length.
     * @param line          The line that is too long.
     */
    record MaxLineLengthExceeded(int maxLineLength, Component line) implements Failure {}

    /**
     * A {@link Failure} returned when the resulting lore exceeds the maximum number of lines.
     *
     * @param maxLines       The maximum number of lines.
     * @param resultingLines The resulting number of lines.
     */
    record MaxLinesExceeded(int maxLines, int resultingLines) implements Failure {}
}
