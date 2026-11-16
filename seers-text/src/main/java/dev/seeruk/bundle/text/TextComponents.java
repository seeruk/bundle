package dev.seeruk.bundle.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Utilities for working with styled text {@link Component}s.
 */
public final class TextComponents {
    private TextComponents() {}

    /**
     * One chunk of text with its style.
     */
    private record Chunk(String text, Style style) {}

    /**
     * Splits text into lines at each line break, while retaining styling, so styled text that spans
     * a line break stays styled on both lines.
     */
    public static List<Component> splitLines(Component text) {
        final var lines = new ArrayList<Component>();
        var line = Component.empty();

        for (final var chunk : chunks(text)) {
            final var parts = chunk.text().split("\n", -1);
            for (var i = 0; i < parts.length; i++) {
                if (i > 0) {
                    lines.add(line);
                    line = Component.empty();
                }
                append(line, parts[i], chunk.style());
            }
        }

        lines.add(line);
        return lines;
    }

    /**
     * Wraps a line of text into lines of at most {@code maxLength} characters (see
     * {@link #length}), keeping its styling. Lines are broken at spaces where possible (dropping
     * the space), and words longer than a whole line are broken wherever they need to be, though
     * never inside a character.
     */
    public static List<Component> wrap(Component line, int maxLength) {
        final var text = line.getString();
        // Where each character starts in the text, and where the text ends
        final var breaks = characterBreaks(text);
        final var characters = breaks.length - 1;
        if (characters <= maxLength) {
            return List.of(line);
        }

        final var chunks = chunks(line);
        final var lines = new ArrayList<Component>();
        var start = 0;

        while (characters - start > maxLength) {
            final var space = lastSpace(text, breaks, start, start + maxLength);
            if (space > start) {
                lines.add(slice(chunks, breaks[start], breaks[space]));
                start = space + 1;
            } else {
                lines.add(slice(chunks, breaks[start], breaks[start + maxLength]));
                start += maxLength;
            }
        }

        lines.add(slice(chunks, breaks[start], text.length()));
        return lines;
    }

    /**
     * How many characters the text shows, so an emoji counts as one, even one made of several
     * others (e.g. a family emoji).
     */
    public static int length(String text) {
        return characterBreaks(text).length - 1;
    }

    private static int[] characterBreaks(String text) {
        final var iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);

        final var breaks = IntStream.builder();
        for (var position = iterator.first(); position != BreakIterator.DONE; position = iterator.next()) {
            breaks.add(position);
        }

        return breaks.build().toArray();
    }

    /**
     * The last character from {@code start} to {@code end} (both inclusive) that's a space, or
     * {@code start} if there isn't one after it.
     */
    private static int lastSpace(String text, int[] breaks, int start, int end) {
        for (var character = end; character > start; character--) {
            // A space on its own, not one with a combining mark on it
            if (text.charAt(breaks[character]) == ' ' && breaks[character + 1] - breaks[character] == 1) {
                return character;
            }
        }

        return start;
    }

    private static List<Chunk> chunks(Component text) {
        final var chunks = new ArrayList<Chunk>();
        text.visit(
            (style, content) -> {
                chunks.add(new Chunk(content, style));
                return Optional.empty();
            }, Style.EMPTY
        );
        return chunks;
    }

    /**
     * Finds the styled text between two positions in a list of chunks' combined plain text.
     *
     * <p>This function is a bit more complex because we want to use offsets across the whole text
     * (i.e. what's represented by the list of chunks), but we have to dive into the chunks to
     * extract it, and we have to do so correctly. Correctly meaning, with the right styling.
     */
    private static Component slice(List<Chunk> chunks, int from, int to) {
        final var slice = Component.empty();
        // Where the current chunk starts in the combined text
        var chunkStart = 0;

        for (final var chunk : chunks) {
            final var length = chunk.text().length();

            // For each chunk, we need to adjust our start and end so that we're pulling out the
            // right text, offsetting it by what we've already pulled out. These are clamped to the
            // size of the chunk (if our from and to span multiple chunks, our chunkStart should
            // fall inside the current chunk, but the end may fall outside it, so it's clamped and
            // the rest falls into the next chunk).
            final var start = Math.clamp(from - chunkStart, 0, length);
            final var end = Math.clamp(to - chunkStart, 0, length);

            append(slice, chunk.text().substring(start, end), chunk.style());
            chunkStart += length;
        }

        return slice;
    }

    private static void append(MutableComponent line, String text, Style style) {
        if (!text.isEmpty()) {
            line.append(Component.literal(text).withStyle(style));
        }
    }
}
