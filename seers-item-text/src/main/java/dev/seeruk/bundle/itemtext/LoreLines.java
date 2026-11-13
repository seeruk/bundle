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
 *
 * <p>Within this utility, line numbers are 1-indexed.
 */
public final class LoreLines {
    public static final int MAX_LINES = 10;
    public static final int MAX_LINE_LENGTH = 60;

    private LoreLines() {}

    /**
     * Appends a line to the end of the lore.
     *
     * @param original The existing lore lines
     * @param line     The line of lore to be appended
     */
    public static LoreEditResult add(List<Component> original, Component line) {
        final var failure = validateLine(line);
        if (failure.isPresent()) {
            return failure.get();
        }

        final var lines = copyLines(original);
        lines.add(line);

        return ensureWithinMaxLines(original, lines);
    }

    /**
     * Set a specified line of lore.
     *
     * @param original   The existing lore lines
     * @param lineNumber The line number to set
     * @param line       The line of lore to set
     */
    public static LoreEditResult set(List<Component> original, int lineNumber, Component line) {
        final var failure = validateLineExists(lineNumber, original.size()).or(() -> validateLine(line));
        if (failure.isPresent()) {
            return failure.get();
        }

        final var lines = copyLines(original);
        lines.set(lineNumber - 1, line);

        return ensureWithinMaxLines(original, lines);
    }

    /**
     * Inserts a line of lore. If the line is occupied, it will be shifted down. If the line does
     * not exist, it will be inserted. If intermediate lines do not exist, they'll be created as
     * blank lines.
     *
     * @param original   The existing lore lines
     * @param lineNumber The line number to insert lore at
     * @param line       The line of lore to insert
     */
    public static LoreEditResult insert(List<Component> original, int lineNumber, Component line) {
        final var failure = validateLineNumber(lineNumber, original.size()).or(() -> validateLine(line));
        if (failure.isPresent()) {
            return failure.get();
        }

        final int resultingLines = Math.max(original.size() + 1, lineNumber);
        if (resultingLines > MAX_LINES) {
            return new LoreEditResult.MaxLinesExceeded(MAX_LINES, resultingLines);
        }

        final var lines = copyLines(original);

        // Insert blank lines if necessary to reach the line number
        while (lines.size() < lineNumber - 1) {
            lines.add(Component.empty());
        }

        // And finally add the line
        lines.add(lineNumber - 1, line);

        return ensureWithinMaxLines(original, lines);
    }

    /**
     * Remove a lore line. {@link #MAX_LINES} is not checked here, so that if an item has more than
     * {@link #MAX_LINES} lines, the lore can still be selectively removed.
     *
     * @param original   The existing lore lines
     * @param lineNumber The line number to remove
     */
    public static LoreEditResult remove(List<Component> original, int lineNumber) {
        final var failure = validateLineExists(lineNumber, original.size());
        if (failure.isPresent()) {
            return failure.get();
        }

        final var lines = copyLines(original);
        lines.remove(lineNumber - 1);

        return new LoreEditResult.Edited(original, lines);
    }

    /**
     * Clear a line of lore (i.e. set it to a blank line). Unlike {@link #remove(List, int)}, this
     * method does check {@link #MAX_LINES}, because this is another form of modifying lore, rather
     * than making it fit within {@link #MAX_LINES}.
     *
     * @param original   The existing lore lines
     * @param lineNumber The line number to clear
     */
    public static LoreEditResult clear(List<Component> original, int lineNumber) {
        final var failure = validateLineExists(lineNumber, original.size());
        if (failure.isPresent()) {
            return failure.get();
        }

        final var lines = copyLines(original);
        lines.set(lineNumber - 1, Component.empty());

        return ensureWithinMaxLines(original, lines);
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

    private static Optional<LoreEditResult.Failure> validateLineNumber(int line, int currentLines) {
        if (line < 1) {
            // Lines are 1-indexed
            return Optional.of(new LoreEditResult.LineOutOfBounds(line, currentLines));
        }

        // There's no check on MAX_LINES here, because it's checked in ensureWithinMaxLines, or for
        // insert as a special case separately.

        return Optional.empty();
    }

    private static Optional<LoreEditResult.Failure> validateLineExists(int lineNumber, int currentLines) {
        final var lineNumberIssue = validateLineNumber(lineNumber, currentLines);
        if (lineNumberIssue.isPresent()) {
            return lineNumberIssue;
        }

        if (currentLines == 0) {
            return Optional.of(new LoreEditResult.NoLore());
        }

        if (lineNumber > currentLines) {
            return Optional.of(new LoreEditResult.LineOutOfBounds(lineNumber, currentLines));
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
