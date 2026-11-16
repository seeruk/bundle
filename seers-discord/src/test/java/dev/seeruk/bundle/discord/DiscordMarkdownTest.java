package dev.seeruk.bundle.discord;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiscordMarkdownTest {
    private static final Style BOLD = Style.EMPTY.withBold(true);
    private static final Style ITALIC = Style.EMPTY.withItalic(true);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> shouldWriteMarkdown() {
        return Stream.of(
            Arguments.of("Keeps plain text as it is", literal("Hello there", Style.EMPTY), "Hello there"),
            Arguments.of(
                "Escapes anything Discord would read as Markdown",
                literal("**hi** _x_ # y - z > q [a] <@1> `c` ||s|| ~~t~~ \\", Style.EMPTY),
                "\\*\\*hi\\*\\* \\_x\\_ \\# y \\- z \\> q \\[a\\] \\<@1\\> \\`c\\` \\|\\|s\\|\\| \\~\\~t\\~\\~ \\\\"
            ),
            Arguments.of(
                "Leaves links as they are",
                literal("See https://example.com/a_b-c", Style.EMPTY),
                "See https://example.com/a_b-c"
            ),
            Arguments.of("Writes bold", literal("hi", BOLD), "**hi**"),
            Arguments.of("Writes italics", literal("hi", ITALIC), "*hi*"),
            Arguments.of("Writes underlines", literal("hi", Style.EMPTY.withUnderlined(true)), "__hi__"),
            Arguments.of("Writes strikethrough", literal("hi", Style.EMPTY.withStrikethrough(true)), "~~hi~~"),
            Arguments.of("Writes bold italics", literal("hi", BOLD.withItalic(true)), "***hi***"),
            Arguments.of("Keeps spaces outside the markers", literal(" hi ", BOLD), " **hi** "),
            Arguments.of("Escapes text inside the markers", literal("a_b", BOLD), "**a\\_b**"),
            Arguments.of("Leaves out colours", literal("hi", Style.EMPTY.withColor(ChatFormatting.RED)), "hi"),
            Arguments.of(
                "Joins neighbouring text with the same style",
                Component.empty().append(literal("a", BOLD)).append(literal("b", BOLD.withColor(ChatFormatting.RED))),
                "**ab**"
            ),
            Arguments.of(
                "Separates neighbouring styles, so their markers aren't read together",
                Component.empty().append(literal("a", BOLD)).append(literal("b", ITALIC)),
                "**a**\u200B*b*"
            ),
            Arguments.of(
                "Doesn't separate styles that already have a space between them",
                Component.empty().append(literal("a ", BOLD)).append(literal("b", ITALIC)),
                "**a** *b*"
            ),
            Arguments.of("Doesn't mark styled spaces", literal(" ", BOLD), " ")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldWriteMarkdown(final String description, final Component text, final String expected) {
        assertEquals(expected, DiscordMarkdown.of(text));
    }

    private static Component literal(String text, Style style) {
        return Component.literal(text).withStyle(style);
    }
}
