package dev.seeruk.bundle.itemtext;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoreEditorTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Parse {
        static Stream<Arguments> shouldParseIntoLines() {
            return Stream.of(
                Arguments.of("No lines for no text", "", List.of()),
                Arguments.of("Splits lines of text a lines of lore", "Testing 1\nTesting 2", List.of("Testing 1", "Testing 2")),
                Arguments.of("Keeps blank lines between lines", "Testing 1\n\nTesting 2", List.of("Testing 1", "", "Testing 2")),
                Arguments.of("Drops blank lines at the end", "Testing 1\n\n \n", List.of("Testing 1")),
                Arguments.of("Doesn't count formatting tags towards a line's length", "<red>" + "a".repeat(LoreLines.MAX_LINE_LENGTH) + "</red>", List.of("a".repeat(LoreLines.MAX_LINE_LENGTH))),
                Arguments.of(
                    "Wraps long lines",
                    "a".repeat(LoreLines.MAX_LINE_LENGTH - 5) + " wrapped words",
                    List.of("a".repeat(LoreLines.MAX_LINE_LENGTH - 5), "wrapped words")
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldParseIntoLines(final String description, final String input, final List<String> expected) {
            assertEquals(expected, LoreEditor.parse(input).stream().map(Component::getString).toList());
        }

        @Test
        void formattingSpansLines() {
            final var lines = LoreEditor.parse("<red>Testing 1\nTesting 2</red>");

            for (final var line : lines) {
                assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), line.toFlatList().getFirst().getStyle().getColor(), line.getString());
            }
        }
    }
}
