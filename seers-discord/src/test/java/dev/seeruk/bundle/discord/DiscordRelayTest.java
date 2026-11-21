package dev.seeruk.bundle.discord;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiscordRelayTest {
    @Test
    void shouldUseThePlayersFaceAsTheirAvatar() {
        final var id = UUID.fromString("0f2c0e5c-6b1a-4c3e-9a63-1d2f3e4a5b6c");

        assertEquals(
            "https://api.mineatar.io/head/0f2c0e5c6b1a4c3e9a631d2f3e4a5b6c?scale=16",
            DiscordRelay.avatarUrl(id)
        );
    }
}
