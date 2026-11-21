package dev.seeruk.bundle.discord;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebhookNamesTest {
    static Stream<Arguments> shouldMakeNamesDiscordAllows() {
        return Stream.of(
            Arguments.of("Keeps an allowed name as it is", "≈Seer", "≈Seer"),
            Arguments.of("Censors Discord in names", "DiscordFan", "D*****dFan"),
            Arguments.of("Censors Clyde in names, ignoring case", "xCLYDEx", "xC***Ex"),
            Arguments.of("Cuts long names down", "a".repeat(81), "a".repeat(80)),
            Arguments.of("Counts emoji by their UTF-16 length", "a".repeat(78) + "😀😀", "a".repeat(78) + "😀"),
            Arguments.of("Doesn't cut an emoji in half", "a".repeat(79) + "😀", "a".repeat(79)),
            Arguments.of("Doesn't cut a joined emoji apart", "a".repeat(78) + "👨\u200D👩\u200D👧", "a".repeat(78))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldMakeNamesDiscordAllows(final String description, final String name, final String expected) {
        assertEquals(expected, WebhookNames.allowed(name));
    }
}
