package dev.seeruk.bundle.text;

import dev.seeruk.bundle.core.testing.TextChunks;
import dev.seeruk.bundle.core.testing.TextChunks.Chunk;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static dev.seeruk.bundle.core.testing.TextChunks.chunk;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextComponentsTest {
    private static final Style RED = Style.EMPTY.withColor(ChatFormatting.RED);
    private static final Style BOLD = Style.EMPTY.withBold(true);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class SplitLines {
        static Stream<Arguments> shouldSplitLines() {
            return Stream.of(
                Arguments.of(
                    "Keeps text without line breaks as one line",
                    literal("Testing", RED),
                    List.of(List.of(chunk("Testing", RED)))
                ),
                Arguments.of(
                    "Keeps the style on both sides of a line break",
                    literal("Testing 1\nTesting 2", RED),
                    List.of(List.of(chunk("Testing 1", RED)), List.of(chunk("Testing 2", RED)))
                ),
                Arguments.of(
                    "Keeps several styles on one line",
                    Component.empty().append(literal("Red ", RED)).append(literal("bold\nBold", BOLD)),
                    List.of(List.of(chunk("Red ", RED), chunk("bold", BOLD)), List.of(chunk("Bold", BOLD)))
                ),
                Arguments.of(
                    "Keeps empty lines",
                    Component.literal("Testing 1\n\nTesting 2"),
                    List.of(
                        List.of(chunk("Testing 1", Style.EMPTY)),
                        List.of(),
                        List.of(chunk("Testing 2", Style.EMPTY))
                    )
                ),
                Arguments.of(
                    "Keeps an empty line after a trailing line break",
                    Component.literal("Testing\n"),
                    List.of(List.of(chunk("Testing", Style.EMPTY)), List.of())
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldSplitLines(final String description, final Component text, final List<List<Chunk>> expected) {
            assertEquals(expected, TextComponents.splitLines(text).stream().map(TextChunks::chunks).toList());
        }
    }

    @Nested
    class Wrap {
        static Stream<Arguments> shouldWrap() {
            return Stream.of(
                Arguments.of(
                    "Keeps a short line as it is",
                    literal("Testing", RED),
                    10,
                    List.of(List.of(chunk("Testing", RED)))
                ),
                Arguments.of(
                    "Keeps a line of exactly the maximum length as it is",
                    Component.literal("Testing 12"),
                    10,
                    List.of(List.of(chunk("Testing 12", Style.EMPTY)))
                ),
                Arguments.of(
                    "Wraps at the last space that fits, dropping it",
                    Component.literal("The quick brown fox"),
                    10,
                    List.of(List.of(chunk("The quick", Style.EMPTY)), List.of(chunk("brown fox", Style.EMPTY)))
                ),
                Arguments.of(
                    "Wraps at a space just past the maximum length",
                    Component.literal("Testing 12 more"),
                    10,
                    List.of(List.of(chunk("Testing 12", Style.EMPTY)), List.of(chunk("more", Style.EMPTY)))
                ),
                Arguments.of(
                    "Breaks a word that's longer than a whole line",
                    Component.literal("Supercalifragilistic"),
                    10,
                    List.of(List.of(chunk("Supercalif", Style.EMPTY)), List.of(chunk("ragilistic", Style.EMPTY)))
                ),
                Arguments.of(
                    "Keeps styles across the wrap point",
                    Component.empty().append(literal("Red text ", RED)).append(literal("bold text", BOLD)),
                    10,
                    List.of(List.of(chunk("Red text", RED)), List.of(chunk("bold text", BOLD)))
                ),
                Arguments.of(
                    "Wraps in the middle of a styled run",
                    literal("One two three four", RED),
                    10,
                    List.of(List.of(chunk("One two", RED)), List.of(chunk("three four", RED)))
                ),
                Arguments.of(
                    "Counts an emoji as one character",
                    Component.literal("😀".repeat(10)),
                    10,
                    List.of(List.of(chunk("😀".repeat(10), Style.EMPTY)))
                ),
                Arguments.of(
                    "Never breaks inside an emoji",
                    Component.literal("a" + "😀".repeat(10)),
                    10,
                    List.of(List.of(chunk("a" + "😀".repeat(9), Style.EMPTY)), List.of(chunk("😀", Style.EMPTY)))
                ),
                Arguments.of(
                    "Never breaks inside an emoji made of others",
                    Component.literal("a".repeat(9) + FAMILY),
                    9,
                    List.of(List.of(chunk("a".repeat(9), Style.EMPTY)), List.of(chunk(FAMILY, Style.EMPTY)))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldWrap(final String description, final Component line, final int maxLength, final List<List<Chunk>> expected) {
            assertEquals(
                expected,
                TextComponents.wrap(line, maxLength).stream().map(TextChunks::chunks).toList()
            );
        }
    }

    @Nested
    class WrapToWidth {
        // Most letters are 6 pixels wide, a space is 4, and an i is 2 (see TextWidths)
        static Stream<Arguments> shouldWrapToWidth() {
            return Stream.of(
                Arguments.of(
                    "Keeps a line of exactly the maximum width as it is",
                    Component.literal("aaa aaa"),
                    40,
                    List.of(List.of(chunk("aaa aaa", Style.EMPTY)))
                ),
                Arguments.of(
                    "Wraps at the last space that fits, dropping it",
                    Component.literal("aaa aaa"),
                    39,
                    List.of(List.of(chunk("aaa", Style.EMPTY)), List.of(chunk("aaa", Style.EMPTY)))
                ),
                Arguments.of(
                    "Fits more narrow characters on a line",
                    Component.literal("iiiiii"),
                    6,
                    List.of(List.of(chunk("iii", Style.EMPTY)), List.of(chunk("iii", Style.EMPTY)))
                ),
                Arguments.of(
                    "Measures bold text as wider",
                    literal("aa", BOLD),
                    13,
                    List.of(List.of(chunk("a", BOLD)), List.of(chunk("a", BOLD)))
                ),
                Arguments.of(
                    "Keeps styles across the wrap point",
                    Component.empty().append(literal("Red ", RED)).append(literal("text", RED.withBold(true))),
                    30,
                    List.of(List.of(chunk("Red", RED)), List.of(chunk("text", RED.withBold(true))))
                ),
                Arguments.of(
                    "Breaks a word wider than a whole line",
                    Component.literal("aaaaa"),
                    18,
                    List.of(List.of(chunk("aaa", Style.EMPTY)), List.of(chunk("aa", Style.EMPTY)))
                ),
                Arguments.of(
                    "Puts a character wider than a whole line on a line of its own",
                    Component.literal("aa"),
                    3,
                    List.of(List.of(chunk("a", Style.EMPTY)), List.of(chunk("a", Style.EMPTY)))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldWrapToWidth(final String description, final Component line, final int maxWidth, final List<List<Chunk>> expected) {
            assertEquals(
                expected,
                TextComponents.wrapToWidth(line, maxWidth).stream().map(TextChunks::chunks).toList()
            );
        }
    }

    @Nested
    class Length {
        static Stream<Arguments> shouldCountCharactersAsShown() {
            return Stream.of(
                Arguments.of("Counts letters", "Testing", 7),
                Arguments.of("Counts an emoji as one", "😀", 1),
                Arguments.of("Counts an emoji made of others as one", FAMILY, 1),
                Arguments.of("Counts nothing in empty text", "", 0)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCountCharactersAsShown(final String description, final String text, final int expected) {
            assertEquals(expected, TextComponents.length(text));
        }
    }

    @Nested
    class Truncate {
        static Stream<Arguments> shouldCutTextDownWithoutSplittingCharacters() {
            return Stream.of(
                Arguments.of("Leaves short text alone", "Testing", 10, "Testing"),
                Arguments.of("Leaves text of exactly the length alone", "Testing", 7, "Testing"),
                Arguments.of("Cuts long text", "Testing", 4, "Test"),
                // The emoji is two UTF-16 units, so the limit falls between them
                Arguments.of("Leaves out an emoji that doesn't fit", "Hi😀", 3, "Hi"),
                Arguments.of("Leaves out an emoji made of others that doesn't fit", "Hi" + FAMILY, 5, "Hi")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCutTextDownWithoutSplittingCharacters(
            final String description,
            final String text,
            final int maxLength,
            final String expected
        ) {
            assertEquals(expected, TextComponents.truncate(text, maxLength));
        }

        static Stream<Arguments> shouldCutStyledTextDownKeepingItsStyles() {
            return Stream.of(
                Arguments.of(
                    "Leaves text of exactly the length alone",
                    literal("Test", RED).append(literal("ing", BOLD)),
                    7,
                    List.of(chunk("Test", RED), chunk("ing", RED.withBold(true)))
                ),
                Arguments.of(
                    "Cuts styled text in the middle of a style",
                    literal("Test", RED).append(literal("ing", BOLD)),
                    5,
                    List.of(chunk("Test", RED), chunk("i", RED.withBold(true)))
                ),
                Arguments.of(
                    "Counts an emoji made of others as one character",
                    literal("Hi" + FAMILY + "!", RED),
                    3,
                    List.of(chunk("Hi" + FAMILY, RED))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCutStyledTextDownKeepingItsStyles(
            final String description,
            final Component text,
            final int maxLength,
            final List<Chunk> expected
        ) {
            assertEquals(expected, TextChunks.chunks(TextComponents.truncate(text, maxLength)));
        }
    }

    // A family emoji, made of three people joined together
    private static final String FAMILY = "👨‍👩‍👧";

    private static MutableComponent literal(String text, Style style) {
        return Component.literal(text).withStyle(style);
    }
}
