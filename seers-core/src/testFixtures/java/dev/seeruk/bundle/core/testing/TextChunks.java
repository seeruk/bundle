package dev.seeruk.bundle.core.testing;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Styled text as each run of text and its style, so tests don't depend on how components are
 * nested.
 */
public final class TextChunks {
    private TextChunks() {}

    public record Chunk(String text, Style style) {}

    public static Chunk chunk(String text, Style style) {
        return new Chunk(text, style);
    }

    public static List<Chunk> chunks(Component text) {
        final var chunks = new ArrayList<Chunk>();
        text.visit(
            (style, content) -> {
                if (!content.isEmpty()) {
                    chunks.add(new Chunk(content, style));
                }
                return Optional.empty();
            }, Style.EMPTY
        );
        return chunks;
    }
}
