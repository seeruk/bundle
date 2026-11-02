package dev.seeruk.bundle.text;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextParsersTest {
    @BeforeAll
    static void bootstrap() {
        // We bootstrap Minecraft, since Text Placeholder API uses registries that need initializing
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class ParseAsTrusted {
        static Stream<Arguments> shouldParse() {
            return Stream.of(
                Arguments.of("<red>Hello</red>", Component.literal("Hello").withStyle(ChatFormatting.RED)),
                Arguments.of("<b>Hello</b>", Component.literal("Hello").withStyle(ChatFormatting.BOLD)),
                Arguments.of(
                    "<run_cmd '/say hi'>Hello</run_cmd>",
                    Component.literal("Hello").withStyle(s -> s.withClickEvent(
                        new ClickEvent.RunCommand("/say hi")
                    ))
                ),
                Arguments.of(
                    "<hover 'Hi'>Hello</hover>",
                    Component.literal("Hello").withStyle(s -> s.withHoverEvent(
                        new HoverEvent.ShowText(Component.literal("Hi"))
                    ))
                ),
                Arguments.of("**Hello**", Component.literal("Hello").withStyle(ChatFormatting.BOLD)),
                Arguments.of("*Hello*", Component.literal("Hello").withStyle(ChatFormatting.ITALIC)),
                // Including "unsafe" Markdown, like links:
                Arguments.of("[Hello](https://example.com)", Component.literal("Hello")
                    .withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE)
                    .withStyle(s -> s.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://example.com"))))
                )
            );
        }

        @ParameterizedTest(name = "shouldParse({0})")
        @MethodSource
        void shouldParse(final String input, final Component expected) {
            assertEquals(expected, TextParsers.parseAsTrusted(input));
        }
    }

    @Nested
    class ParseAsUntrusted {
        static Stream<Arguments> shouldParse() {
            return Stream.of(
                Arguments.of("<red>Hello</red>", Component.literal("Hello").withStyle(ChatFormatting.RED)),
                Arguments.of("<b>Hello</b>", Component.literal("Hello").withStyle(ChatFormatting.BOLD)),
                Arguments.of("**Hello**", Component.literal("Hello").withStyle(ChatFormatting.BOLD)),
                Arguments.of("*Hello*", Component.literal("Hello").withStyle(ChatFormatting.ITALIC)),
                Arguments.of("__Hello__", Component.literal("Hello").withStyle(ChatFormatting.UNDERLINE)),
                Arguments.of("~~Hello~~", Component.literal("Hello").withStyle(ChatFormatting.STRIKETHROUGH))
            );
        }

        static Stream<Arguments> shouldNotParse() {
            return Stream.of(
                Arguments.of("<page 2>Test</page>"),
                Arguments.of("<run_cmd '/say hi'>Hello</run_cmd>"),
                Arguments.of("<hover 'Hi'>Hello</hover>"),
                Arguments.of("`Hello`"),
                Arguments.of("[Hello](https://example.com)")
            );
        }

        @ParameterizedTest(name = "shouldParse({0})")
        @MethodSource
        void shouldParse(final String input, final Component expected) {
            assertEquals(expected, TextParsers.parseAsUntrusted(input));
        }

        @ParameterizedTest(name = "shouldNotParse({0})")
        @MethodSource
        void shouldNotParse(final String input) {
            assertEquals(Component.literal(input), TextParsers.parseAsUntrusted(input));
        }
    }
}
