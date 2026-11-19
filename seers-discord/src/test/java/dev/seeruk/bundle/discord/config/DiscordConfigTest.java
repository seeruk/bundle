package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DiscordConfigTest {
    private static final Map<String, Object> EVENT = Map.of(
        "enabled", true,
        "format", "${display_name}",
        "colour", "yellow"
    );

    // Every event, as it's written in config
    private static final Map<String, Object> EVENTS = Map.of(
        "join", EVENT,
        "quit", EVENT,
        "death", EVENT,
        "advancement", Map.of(
            "enabled", true,
            "format", "${message}",
            "description_format", "${description}",
            "summary_format", "${display_name} completed ${count} advancements",
            "colour", "green",
            "challenge_colour", "dark_purple"
        ),
        "afk", EVENT,
        "afk_return", EVENT,
        "server_start", EVENT,
        "server_stop", EVENT
    );

    static Stream<Arguments> shouldReadChannelIds() {
        return Stream.of(
            Arguments.of("Reads an ID written as text", "1234567890123456789", "1234567890123456789"),
            Arguments.of("Reads an ID written as a number", 1234567890123456789L, "1234567890123456789"),
            Arguments.of("Reads a blank ID, as it's not set yet", "", "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldReadChannelIds(final String description, final Object channelId, final String expected) {
        assertEquals(expected, parse(channelId).getOrThrow().channelId());
    }

    @Test
    void shouldRefuseChannelIdsThatAreNotIds() {
        assertTrue(parse("#general").isError());
    }

    @Test
    void shouldRefuseChannelIdsTooLongToBeIds() {
        // e.g. two IDs pasted together
        assertTrue(parse("12345678901234567891234567890123456789").isError());
    }

    @Test
    void shouldHideTheTokenWhenPrinted() {
        final var parsed = parse("123").getOrThrow();
        final var config = new DiscordConfig("secret-token", "123", parsed.discord(), parsed.game());

        assertFalse(config.toString().contains("secret-token"));
    }

    private static DataResult<DiscordConfig> parse(Object channelId) {
        return DiscordConfig.CODEC.parse(
            JavaOps.INSTANCE, Map.of(
                "bot_token", "token",
                "channel_id", channelId,
                "discord", Map.of(
                    "server", Map.of("name", "Minecraft Server", "avatar", ""),
                    "chat", "${message}",
                    "emote", "_${message}_",
                    "events", EVENTS,
                    "commands", Map.of(
                        "public_replies", false,
                        "players", Map.of("enabled", true),
                        "status", Map.of("enabled", true)
                    )
                ),
                "game", Map.of("chat", "${display_name}: ${message}", "reply", "${message}")
            )
        );
    }
}
