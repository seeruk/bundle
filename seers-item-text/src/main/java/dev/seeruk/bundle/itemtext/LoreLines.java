package dev.seeruk.bundle.itemtext;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LoreLines is a utility class that provides constants and methods related to the lines of lore,
 * represented by a {@code List<Component>}. Critically, this class doesn't use anything related to
 * {@code Player} or {@code ItemStack}.
 */
public final class LoreLines {
    public static final int MAX_LINES = 10;
    public static final int MAX_LINE_LENGTH = 60;

    private LoreLines() {}

    /**
     * Replaces all the lore with the given lines.
     *
     * @param original The existing lore lines
     * @param lines    The lines of lore to replace them with
     */
    public static LoreEditResult replace(List<Component> original, List<Component> lines) {
        for (final var line : lines) {
            final var failure = validateLine(line);
            if (failure.isPresent()) {
                return failure.get();
            }
        }

        return ensureWithinMaxLines(original, copyLines(lines));
    }

    /**
     * Copies a list of components into a mutable ArrayList, ready for working on.
     *
     * @param lines The lines to be copied
     */
    private static ArrayList<Component> copyLines(List<Component> lines) {
        return lines.stream()
            .map(Component::copy)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Optional<LoreEditResult.Failure> validateLine(Component line) {
        final var text = line.getString();
        if (text.length() > MAX_LINE_LENGTH) {
            return Optional.of(new LoreEditResult.MaxLineLengthExceeded(MAX_LINE_LENGTH, line));
        }

        return Optional.empty();
    }

    private static LoreEditResult ensureWithinMaxLines(List<Component> original, List<Component> edited) {
        if (edited.size() > MAX_LINES) {
            return new LoreEditResult.MaxLinesExceeded(MAX_LINES, edited.size());
        }

        return new LoreEditResult.Edited(original, edited);
    }
}
