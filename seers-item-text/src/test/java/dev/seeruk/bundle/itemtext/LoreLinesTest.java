package dev.seeruk.bundle.itemtext;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoreLinesTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Add {
        @FunctionalInterface
        interface Expected extends BiFunction<List<Component>, Component, LoreEditResult> {}

        static Stream<Arguments> shouldAppendLine() {
            return Stream.of(
                // Append to an empty list
                Arguments.of(
                    List.of(),
                    literal("Testing"),
                    List.of(literal("Testing"))
                ),
                // Append to end of existing list
                Arguments.of(
                    List.of(literal("Testing")),
                    literal("Testing"),
                    List.of(literal("Testing"), literal("Testing"))
                ),
                // Allows the maximum line length
                Arguments.of(
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH)),
                    List.of(literal("a".repeat(LoreLines.MAX_LINE_LENGTH)))
                ),
                // Styling doesn't count towards line length
                Arguments.of(
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH)).withStyle(ChatFormatting.RED),
                    List.of(literal("a".repeat(LoreLines.MAX_LINE_LENGTH)).withStyle(ChatFormatting.RED))
                ),
                // Allows an empty line
                Arguments.of(
                    List.of(),
                    literal(""),
                    List.of(literal(""))
                ),
                // Allows the maximum number of lines
                Arguments.of(
                    nLines(LoreLines.MAX_LINES - 1, i -> literal("%d".formatted(i))),
                    literal("%d".formatted(LoreLines.MAX_LINES - 1)),
                    nLines(LoreLines.MAX_LINES, i -> literal("%d".formatted(i)))
                )
            );
        }

        @ParameterizedTest(name = "shouldAppendLine({0}, {1})")
        @MethodSource
        void shouldAppendLine(final List<Component> lines, final Component line, final List<Component> expected) {
            assertEquals(new LoreEditResult.Edited(lines, expected), LoreLines.add(lines, line));
        }

        static Stream<Arguments> shouldNotAppendLine() {
            return Stream.of(
                // Does not allow several sub-maximum length component parts in one tree
                Arguments.of(
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH / 2))
                        .append(literal("a".repeat((LoreLines.MAX_LINE_LENGTH / 2) + 1))),
                    (Expected) (lines, line) ->
                        new LoreEditResult.InvalidLineLength(LoreLines.MAX_LINE_LENGTH, line)
                ),
                // Does not allow a line beyond the maximum line length
                Arguments.of(
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH + 1)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.InvalidLineLength(LoreLines.MAX_LINE_LENGTH, line)
                ),
                // Does not allow appending past the maximum number of lines
                Arguments.of(
                    nLines(LoreLines.MAX_LINES, i -> literal("%d".formatted(i))),
                    literal("%d".formatted(LoreLines.MAX_LINES)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, LoreLines.MAX_LINES + 1)
                ),
                // Does not allow appending to lines above the maximum number of lines already
                Arguments.of(
                    nLines(LoreLines.MAX_LINES + 10, i -> literal("%d".formatted(i))),
                    literal("%d".formatted(LoreLines.MAX_LINES)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, LoreLines.MAX_LINES + 11)
                )
            );
        }

        @ParameterizedTest(name = "shouldNotAppendLine({0}, {1})")
        @MethodSource
        void shouldNotAppendLine(final List<Component> lines, final Component line, final Expected expected) {
            assertEquals(expected.apply(lines, line), LoreLines.add(lines, line));
        }
    }

    private static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    private static List<Component> nLines(int lines, IntFunction<Component> lineGenerator) {
        return IntStream.range(0, lines)
            .mapToObj(lineGenerator)
            .toList();
    }
}
