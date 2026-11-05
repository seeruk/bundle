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
                Arguments.of(
                    "Appends to an empty list",
                    List.of(),
                    literal("Testing"),
                    List.of(literal("Testing"))
                ),
                Arguments.of(
                    "Appends to end of existing list",
                    List.of(literal("Testing 1")),
                    literal("Testing 2"),
                    List.of(literal("Testing 1"), literal("Testing 2"))
                ),
                Arguments.of(
                    "Preserves styling and children of existing lines",
                    List.of(styledLine()),
                    literal("Testing"),
                    List.of(styledLine(), literal("Testing"))
                ),
                Arguments.of(
                    "Allows the maximum line length",
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH)),
                    List.of(literal("a".repeat(LoreLines.MAX_LINE_LENGTH)))
                ),
                Arguments.of(
                    "Styling doesn't count towards line length",
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH)).withStyle(ChatFormatting.RED),
                    List.of(literal("a".repeat(LoreLines.MAX_LINE_LENGTH)).withStyle(ChatFormatting.RED))
                ),
                Arguments.of(
                    "Allows an empty line",
                    List.of(),
                    literal(""),
                    List.of(literal(""))
                ),
                Arguments.of(
                    "Allows the maximum number of lines",
                    numberedLines(LoreLines.MAX_LINES - 1),
                    literal("%d".formatted(LoreLines.MAX_LINES - 1)),
                    numberedLines(LoreLines.MAX_LINES)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldAppendLine(
            final String description,
            final List<Component> lines,
            final Component line,
            final List<Component> expected
        ) {
            assertEquals(new LoreEditResult.Edited(lines, expected), LoreLines.add(lines, line));
        }

        static Stream<Arguments> shouldNotAppendLine() {
            return Stream.of(
                Arguments.of(
                    "Does not allow several sub-maximum length component parts in one tree",
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH / 2))
                        .append(literal("a".repeat((LoreLines.MAX_LINE_LENGTH / 2) + 1))),
                    (Expected) (lines, line) ->
                        new LoreEditResult.InvalidLineLength(LoreLines.MAX_LINE_LENGTH, line)
                ),
                Arguments.of(
                    "Does not allow a line beyond the maximum line length",
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH + 1)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.InvalidLineLength(LoreLines.MAX_LINE_LENGTH, line)
                ),
                Arguments.of(
                    "Does not allow appending past the maximum number of lines",
                    numberedLines(LoreLines.MAX_LINES),
                    literal("%d".formatted(LoreLines.MAX_LINES)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size() + 1)
                ),
                Arguments.of(
                    "Does not allow appending to lines above the maximum number of lines already",
                    numberedLines(LoreLines.MAX_LINES + 10),
                    literal("%d".formatted(LoreLines.MAX_LINES)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size() + 1)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldNotAppendLine(
            final String description,
            final List<Component> lines,
            final Component line,
            final Expected expected
        ) {
            assertEquals(expected.apply(lines, line), LoreLines.add(lines, line));
        }
    }

    private static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    private static Component styledLine() {
        return literal("Red").withStyle(ChatFormatting.RED)
            .append(literal(" with bold").withStyle(ChatFormatting.BOLD));
    }

    private static List<Component> numberedLines(int count) {
        return IntStream.range(0, count)
            .<Component>mapToObj(i -> literal("%d".formatted(i)))
            .toList();
    }
}
