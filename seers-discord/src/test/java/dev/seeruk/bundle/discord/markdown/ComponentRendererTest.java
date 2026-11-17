package dev.seeruk.bundle.discord.markdown;

import dev.seeruk.bundle.core.testing.TextChunks;
import dev.seeruk.bundle.core.testing.TextChunks.Chunk;
import dev.seeruk.bundle.text.TextColours;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.seeruk.bundle.core.testing.TextChunks.chunk;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComponentRendererTest {
    private static final Style BOLD = Style.EMPTY.withBold(true);
    private static final Style ITALIC = Style.EMPTY.withItalic(true);
    private static final Style GRAY = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style GREEN = Style.EMPTY.withColor(ChatFormatting.GREEN);

    private static final Mentions MENTIONS = new Mentions(
        Map.of(123L, "Seer"),
        Map.of(5L, "Admin"),
        Map.of(7L, "general")
    );

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> shouldParseMarkdown() {
        return Stream.of(
            Arguments.of("Keeps plain text as it is", "Hello World!", List.of(plain("Hello World!"))),
            Arguments.of("Leaves the game's tags as written", "<red>hi</red>", List.of(plain("<red>hi</red>"))),
            Arguments.of(
                "Removes the game's formatting codes",
                "\u00A7cred `\u00A7kcode` <@123>",
                List.of(plain("cred "), chunk("kcode", GRAY), plain(" "), chunk("@Seer", GREEN))
            ),

            // Styles
            Arguments.of("Parses bold", "**hi**", List.of(chunk("hi", BOLD))),
            Arguments.of("Parses italics with asterisks", "*hi*", List.of(chunk("hi", ITALIC))),
            Arguments.of("Parses italics with underscores", "_hi_", List.of(chunk("hi", ITALIC))),
            Arguments.of("Parses underlines", "__hi__", List.of(chunk("hi", Style.EMPTY.withUnderlined(true)))),
            Arguments.of("Parses strikethrough", "~~hi~~", List.of(chunk("hi", Style.EMPTY.withStrikethrough(true)))),
            Arguments.of("Parses bold italics", "***hi***", List.of(chunk("hi", BOLD.withItalic(true)))),
            Arguments.of(
                "Parses italics inside bold",
                "**a *b* c**",
                List.of(chunk("a ", BOLD), chunk("b", BOLD.withItalic(true)), chunk(" c", BOLD))
            ),
            Arguments.of(
                "Parses bold inside italics",
                "*a **b** c*",
                List.of(chunk("a ", ITALIC), chunk("b", ITALIC.withBold(true)), chunk(" c", ITALIC))
            ),
            Arguments.of(
                "Keeps text around styles",
                "a **b** c",
                List.of(plain("a "), chunk("b", BOLD), plain(" c"))
            ),
            Arguments.of("Leaves unclosed markers as written", "**hi", List.of(plain("**hi"))),
            Arguments.of(
                "Tries shorter markers when longer ones aren't closed",
                "**hi*",
                List.of(plain("*"), chunk("hi", ITALIC))
            ),
            Arguments.of("Leaves underscores inside words", "snake_case_name", List.of(plain("snake_case_name"))),
            Arguments.of(
                "Parses italics with underscores starting inside a word",
                "snake_case_",
                List.of(plain("snake"), chunk("case", ITALIC))
            ),
            Arguments.of("Leaves underscores ending inside a word", "_snake_case", List.of(plain("_snake_case"))),
            Arguments.of(
                "Parses italics with underscores from the last underscore that can start them",
                "a_b_c_",
                List.of(plain("a_b"), chunk("c", ITALIC))
            ),
            Arguments.of("Parses bold starting with a space", "** hi**", List.of(chunk(" hi", BOLD))),
            Arguments.of(
                "Parses italics with asterisks starting inside a word",
                "snake*case*",
                List.of(plain("snake"), chunk("case", ITALIC))
            ),
            Arguments.of("Parses italics with asterisks ending with a space", "*hi *", List.of(chunk("hi ", ITALIC))),
            Arguments.of(
                "Parses italics with asterisks ending inside a word",
                "*snake*case",
                List.of(chunk("snake", ITALIC), plain("case"))
            ),
            Arguments.of("Leaves markers followed by a space", "a * b * c", List.of(plain("a * b * c"))),
            Arguments.of("Leaves escaped markers as written", "\\*hi\\*", List.of(plain("*hi*"))),

            // Code
            Arguments.of("Shows code as written", "`**hi**`", List.of(chunk("**hi**", GRAY))),
            Arguments.of("Shows code with two backticks", "``a`b``", List.of(chunk("a`b", GRAY))),
            Arguments.of("Shows code blocks as written", "```java\nint x;\n```", List.of(chunk("int x;", GRAY))),

            // Lines
            Arguments.of("Keeps lines", "a\nb", List.of(plain("a\nb"))),
            Arguments.of("Makes headings bold", "# Title", List.of(chunk("Title", BOLD))),
            Arguments.of("Makes small text grey", "-# small", List.of(chunk("small", GRAY))),
            Arguments.of("Makes quotes grey", "> quoted", List.of(chunk("quoted", GRAY))),
            Arguments.of(
                "Makes everything after a long quote grey",
                ">>> a\nb",
                List.of(chunk("a\nb", GRAY))
            ),
            Arguments.of("Turns list items into bullets", "- item", List.of(plain("• item"))),
            Arguments.of("Turns list items with asterisks into bullets", "* item", List.of(plain("• item"))),

            // Spoilers
            Arguments.of(
                "Hides spoilers until they're hovered over",
                "||secret||",
                List.of(chunk(
                    "[spoiler]",
                    Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)
                        .withHoverEvent(new HoverEvent.ShowText(Component.empty().append("secret")))
                ))
            ),

            // Links
            Arguments.of(
                "Links masked links",
                "[Example](https://example.com)",
                List.of(chunk("Example", link("https://example.com")))
            ),
            Arguments.of(
                "Links links, without the sentence's punctuation",
                "See https://example.com/a_b.",
                List.of(plain("See "), chunk("https://example.com/a_b", link("https://example.com/a_b")), plain("."))
            ),
            Arguments.of(
                "Links links in angle brackets",
                "<https://example.com>",
                List.of(chunk("https://example.com", link("https://example.com")))
            ),

            // Mentions and emoji
            Arguments.of("Names mentioned users", "<@123>", List.of(chunk("@Seer", GREEN))),
            Arguments.of("Names mentioned users by nickname mention", "<@!123>", List.of(chunk("@Seer", GREEN))),
            Arguments.of("Names mentioned roles", "<@&5>", List.of(chunk("@Admin", GREEN))),
            Arguments.of("Names mentioned channels", "<#7>", List.of(chunk("#general", GREEN))),
            Arguments.of("Shows unknown users", "<@999>", List.of(chunk("@unknown-user", GREEN))),
            Arguments.of(
                "Shows users with IDs too long to be real as unknown",
                "<@99999999999999999999999>",
                List.of(chunk("@unknown-user", GREEN))
            ),
            Arguments.of("Names custom emoji", "<:pepe:123>", List.of(plain(":pepe:"))),
            Arguments.of("Names animated custom emoji", "<a:dance:123>", List.of(plain(":dance:"))),
            Arguments.of("Names emoji the game can't show", "Hi 😄", List.of(plain("Hi :smile:")))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldParseMarkdown(final String description, final String markdown, final List<Chunk> expected) {
        assertEquals(expected, TextChunks.chunks(ComponentRenderer.render(markdown, MENTIONS)));
    }

    private static Chunk plain(String text) {
        return chunk(text, Style.EMPTY);
    }

    private static Style link(String url) {
        return Style.EMPTY.withColor(TextColours.CLICKABLE)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)));
    }
}
