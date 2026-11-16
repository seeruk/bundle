package dev.seeruk.bundle.text;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
            assertEquals(expected, TextComponents.splitLines(text).stream().map(TextComponentsTest::chunks).toList());
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
                TextComponents.wrap(line, maxLength).stream().map(TextComponentsTest::chunks).toList()
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

    // A family emoji, made of three people joined together
    private static final String FAMILY = "👨‍👩‍👧";

    // Each run of text and its style, so tests don't depend on how components are nested
    record Chunk(String text, Style style) {}

    private static Chunk chunk(String text, Style style) {
        return new Chunk(text, style);
    }

    private static MutableComponent literal(String text, Style style) {
        return Component.literal(text).withStyle(style);
    }

    private static List<Chunk> chunks(Component text) {
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
