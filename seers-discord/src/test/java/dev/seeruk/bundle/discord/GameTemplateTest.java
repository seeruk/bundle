package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.testing.TextChunks;
import dev.seeruk.bundle.core.testing.TextChunks.Chunk;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.seeruk.bundle.core.testing.TextChunks.chunk;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameTemplateTest {
    private static final Style BLUE = Style.EMPTY.withColor(ChatFormatting.BLUE);
    private static final Style AQUA = Style.EMPTY.withColor(ChatFormatting.AQUA);

    private static final Map<String, Component> VALUES = Map.of(
        "display_name", Component.literal("Seer"),
        "username", Component.literal("seeruk"),
        "message", Component.literal("Hello").withStyle(ChatFormatting.BOLD)
    );

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> shouldFormat() {
        return Stream.of(
            Arguments.of(
                "Fills in placeholders, keeping their values' styles",
                "${display_name} (${username}): ${message}",
                VALUES,
                List.of(
                    plain("Seer"),
                    plain(" ("),
                    plain("seeruk"),
                    plain("): "),
                    chunk("Hello", Style.EMPTY.withBold(true))
                )
            ),
            Arguments.of(
                "Styles the template with its tags",
                "<blue>[Discord]</blue> <aqua>${display_name}</aqua>: ${message}",
                VALUES,
                List.of(
                    chunk("[Discord]", BLUE),
                    plain(" "),
                    chunk("Seer", AQUA),
                    plain(": "),
                    chunk("Hello", Style.EMPTY.withBold(true))
                )
            ),
            Arguments.of(
                "Shows values as they are, rather than as tags",
                "${message}",
                Map.of("message", Component.literal("<red>Hello</red>")),
                List.of(plain("<red>Hello</red>"))
            ),
            Arguments.of(
                "Leaves unknown placeholders as written",
                "${nope} ${message}",
                VALUES,
                List.of(plain("${nope} "), chunk("Hello", Style.EMPTY.withBold(true)))
            ),
            Arguments.of(
                "Leaves placeholders without a value as written",
                "${username}: ${message}",
                Map.of("message", Component.literal("Hello")),
                List.of(plain("${username}"), plain(": "), plain("Hello"))
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldFormat(
        final String description,
        final String template,
        final Map<String, Component> values,
        final List<Chunk> expected
    ) {
        assertEquals(expected, TextChunks.chunks(new GameTemplate(template).format(values)));
    }

    private static Chunk plain(String text) {
        return chunk(text, Style.EMPTY);
    }
}
