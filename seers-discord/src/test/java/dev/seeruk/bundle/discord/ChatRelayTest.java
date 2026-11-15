package dev.seeruk.bundle.discord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatRelayTest {
    static Stream<Arguments> shouldMakeNamesDiscordAllows() {
        return Stream.of(
            Arguments.of("Keeps an allowed name as it is", "≈Seer", "≈Seer"),
            Arguments.of("Censors Discord in names", "DiscordFan", "D*****dFan"),
            Arguments.of("Censors Clyde in names, ignoring case", "xCLYDEx", "xC***Ex"),
            Arguments.of("Cuts long names down", "a".repeat(81), "a".repeat(80)),
            Arguments.of("Doesn't cut an emoji in half", "a".repeat(79) + "😀😀", "a".repeat(79) + "😀")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldMakeNamesDiscordAllows(final String description, final String name, final String expected) {
        assertEquals(expected, ChatRelay.username(name));
    }

    @Test
    void shouldUseThePlayersFaceAsTheirAvatar() {
        final var id = UUID.fromString("0f2c0e5c-6b1a-4c3e-9a63-1d2f3e4a5b6c");

        assertEquals("https://api.mineatar.io/head/0f2c0e5c6b1a4c3e9a631d2f3e4a5b6c?scale=16", ChatRelay.avatarUrl(id));
    }
}
