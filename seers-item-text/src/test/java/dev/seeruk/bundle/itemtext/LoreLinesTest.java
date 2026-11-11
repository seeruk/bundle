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
                        new LoreEditResult.MaxLineLengthExceeded(LoreLines.MAX_LINE_LENGTH, line)
                ),
                Arguments.of(
                    "Does not allow a line beyond the maximum line length",
                    List.of(),
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH + 1)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.MaxLineLengthExceeded(LoreLines.MAX_LINE_LENGTH, line)
                ),
                Arguments.of(
                    "Does not allow appending past the maximum number of lines",
                    numberedLines(LoreLines.MAX_LINES),
                    literal("%d".formatted(LoreLines.MAX_LINES)),
                    (Expected) (lines, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size() + 1)
                ),
                Arguments.of(
                    "Does not allow appending to lines while above the maximum number of lines already",
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

    @Nested
    class Set {
        @FunctionalInterface
        interface Expected {
            LoreEditResult apply(List<Component> lines, int lineNumber, Component line);
        }

        static Stream<Arguments> shouldSetLine() {
            return Stream.of(
                Arguments.of(
                    "Sets an existing line without affecting other lines",
                    List.of(literal("Testing 1"), literal("Testing 2"), literal("Testing 3")),
                    2,
                    literal("Testing 4"),
                    List.of(literal("Testing 1"), literal("Testing 4"), literal("Testing 3"))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldSetLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final Component line,
            final List<Component> expected
        ) {
            assertEquals(new LoreEditResult.Edited(lines, expected), LoreLines.set(lines, lineNumber, line));
        }

        static Stream<Arguments> shouldNotSetLine() {
            return Stream.of(
                Arguments.of(
                    "Does not allow setting a line when there is no lore",
                    List.of(),
                    1,
                    literal("Testing"),
                    (Expected) (lines, lineNumber, line) -> new LoreEditResult.NoLore()
                ),
                Arguments.of(
                    "Does not allow setting line zero",
                    List.of(literal("Testing 1")),
                    0,
                    literal("Testing 2"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                ),
                Arguments.of(
                    "Does not allow setting a line past the last line",
                    List.of(literal("Testing 1")),
                    2,
                    literal("Testing 2"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                ),
                Arguments.of(
                    "Does not allow a line beyond the maximum line length",
                    List.of(literal("Testing")),
                    1,
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH + 1)),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.MaxLineLengthExceeded(LoreLines.MAX_LINE_LENGTH, line)
                ),
                Arguments.of(
                    "Does not allow setting a line when above the maximum number of lines already",
                    numberedLines(LoreLines.MAX_LINES + 10),
                    1,
                    literal("Testing"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size())
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldNotSetLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final Component line,
            final Expected expected
        ) {
            assertEquals(expected.apply(lines, lineNumber, line), LoreLines.set(lines, lineNumber, line));
        }
    }

    @Nested
    class Insert {
        @FunctionalInterface
        interface Expected {
            LoreEditResult apply(List<Component> lines, int lineNumber, Component line);
        }

        static Stream<Arguments> shouldInsertLine() {
            return Stream.of(
                Arguments.of(
                    "Allows inserting a line an existing index, pushing subsequent lines down",
                    List.of(literal("Testing 1"), literal("Testing 2")),
                    1,
                    literal("Testing 3"),
                    List.of(literal("Testing 3"), literal("Testing 1"), literal("Testing 2"))
                ),
                Arguments.of(
                    "Allows inserting directly after the last line",
                    List.of(literal("Testing 1")),
                    2,
                    literal("Testing 2"),
                    List.of(literal("Testing 1"), literal("Testing 2"))
                ),
                Arguments.of(
                    "Allows inserting at any line, padding with blank lines on the way",
                    List.of(literal("Testing 1")),
                    4,
                    literal("Testing 2"),
                    List.of(literal("Testing 1"), Component.empty(), Component.empty(), literal("Testing 2"))
                ),
                Arguments.of(
                    "Allows inserting up to the maximum number of lines",
                    numberedLines(LoreLines.MAX_LINES - 1),
                    1,
                    literal("Testing"),
                    Stream.concat(Stream.of(literal("Testing")), numberedLines(LoreLines.MAX_LINES - 1).stream())
                        .toList()
                ),
                Arguments.of(
                    "Allows padding up to the maximum number of lines",
                    List.of(),
                    LoreLines.MAX_LINES,
                    literal("Testing"),
                    Stream.concat(
                        Stream.generate(Component::empty).limit(LoreLines.MAX_LINES - 1),
                        Stream.of(literal("Testing"))
                    ).toList()
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldInsertLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final Component line,
            final List<Component> expected
        ) {
            assertEquals(new LoreEditResult.Edited(lines, expected), LoreLines.insert(lines, lineNumber, line));
        }

        static Stream<Arguments> shouldNotInsertLine() {
            return Stream.of(
                Arguments.of(
                    "Does not allow inserting at line zero",
                    List.of(literal("Testing 1")),
                    0,
                    literal("Testing 2"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                ),
                Arguments.of(
                    "Does not allow a line beyond the maximum line length",
                    List.of(),
                    1,
                    literal("a".repeat(LoreLines.MAX_LINE_LENGTH + 1)),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.MaxLineLengthExceeded(LoreLines.MAX_LINE_LENGTH, line)
                ),
                Arguments.of(
                    "Does not allow inserting past the maximum number of lines",
                    numberedLines(LoreLines.MAX_LINES),
                    1,
                    literal("Testing"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size() + 1)
                ),
                Arguments.of(
                    "Does not allow padding past the maximum number of lines",
                    List.of(),
                    LoreLines.MAX_LINES + 1,
                    literal("Testing"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lineNumber)
                ),
                Arguments.of(
                    "Does not allow inserting into lines while above the maximum number of lines already",
                    numberedLines(LoreLines.MAX_LINES + 10),
                    3,
                    literal("Testing"),
                    (Expected) (lines, lineNumber, line) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size() + 1)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldNotInsertLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final Component line,
            final Expected expected
        ) {
            assertEquals(expected.apply(lines, lineNumber, line), LoreLines.insert(lines, lineNumber, line));
        }
    }

    @Nested
    class Remove {
        @FunctionalInterface
        interface Expected extends BiFunction<List<Component>, Integer, LoreEditResult> {}

        static Stream<Arguments> shouldRemoveLine() {
            return Stream.of(
                Arguments.of(
                    "Removes a line, shifting subsequent lines up",
                    List.of(literal("Testing 1"), literal("Testing 2"), literal("Testing 3")),
                    2,
                    List.of(literal("Testing 1"), literal("Testing 3"))
                ),
                Arguments.of(
                    "Allows removing a line when already above the maximum number of lines",
                    numberedLines(LoreLines.MAX_LINES + 10),
                    LoreLines.MAX_LINES + 10,
                    numberedLines(LoreLines.MAX_LINES + 9)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRemoveLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final List<Component> expected
        ) {
            assertEquals(new LoreEditResult.Edited(lines, expected), LoreLines.remove(lines, lineNumber));
        }

        static Stream<Arguments> shouldNotRemoveLine() {
            return Stream.of(
                Arguments.of(
                    "Does not allow removing a line when there is no lore",
                    List.of(),
                    1,
                    (Expected) (lines, lineNumber) -> new LoreEditResult.NoLore()
                ),
                Arguments.of(
                    "Does not allow removing line zero",
                    List.of(literal("Testing")),
                    0,
                    (Expected) (lines, lineNumber) -> new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                ),
                Arguments.of(
                    "Does not allow removing a line past the last line",
                    List.of(literal("Testing")),
                    2,
                    (Expected) (lines, lineNumber) -> new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldNotRemoveLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final Expected expected
        ) {
            assertEquals(expected.apply(lines, lineNumber), LoreLines.remove(lines, lineNumber));
        }
    }

    @Nested
    class Clear {
        @FunctionalInterface
        interface Expected extends BiFunction<List<Component>, Integer, LoreEditResult> {}

        static Stream<Arguments> shouldClearLine() {
            return Stream.of(
                Arguments.of(
                    "Clears a line, leaving a blank line behind",
                    List.of(literal("Testing 1"), literal("Testing 2"), literal("Testing 3")),
                    2,
                    List.of(literal("Testing 1"), Component.empty(), literal("Testing 3"))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldClearLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final List<Component> expected
        ) {
            assertEquals(new LoreEditResult.Edited(lines, expected), LoreLines.clear(lines, lineNumber));
        }

        static Stream<Arguments> shouldNotClearLine() {
            return Stream.of(
                Arguments.of(
                    "Does not allow clearing a line when there is no lore",
                    List.of(),
                    1,
                    (Expected) (lines, lineNumber) -> new LoreEditResult.NoLore()
                ),
                Arguments.of(
                    "Does not allow clearing line zero",
                    List.of(literal("Testing")),
                    0,
                    (Expected) (lines, lineNumber) -> new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                ),
                Arguments.of(
                    "Does not allow clearing a line past the last line",
                    List.of(literal("Testing")),
                    2,
                    (Expected) (lines, lineNumber) -> new LoreEditResult.LineOutOfBounds(lineNumber, lines.size())
                ),
                Arguments.of(
                    "Does not allow clearing a line while above the maximum number of lines already",
                    numberedLines(LoreLines.MAX_LINES + 10),
                    1,
                    (Expected) (lines, lineNumber) ->
                        new LoreEditResult.MaxLinesExceeded(LoreLines.MAX_LINES, lines.size())
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldNotClearLine(
            final String description,
            final List<Component> lines,
            final int lineNumber,
            final Expected expected
        ) {
            assertEquals(expected.apply(lines, lineNumber), LoreLines.clear(lines, lineNumber));
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
