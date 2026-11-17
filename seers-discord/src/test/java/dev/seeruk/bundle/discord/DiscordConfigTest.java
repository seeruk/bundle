package dev.seeruk.bundle.discord;

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
    private static final DiscordConfig.Formats FORMATS = new DiscordConfig.Formats(
        new DiscordConfig.DiscordFormats(new DiscordTemplate("${message}"), new DiscordTemplate("_${message}_")),
        new DiscordConfig.GameFormats(new GameTemplate("${display_name}: ${message}"))
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
    void shouldHideTheTokenWhenPrinted() {
        final var config = new DiscordConfig("secret-token", "123", FORMATS);

        assertFalse(config.toString().contains("secret-token"));
    }

    private static DataResult<DiscordConfig> parse(Object channelId) {
        return DiscordConfig.CODEC.parse(
            JavaOps.INSTANCE, Map.of(
                "bot_token", "token",
                "channel_id", channelId,
                "formats", Map.of(
                    "discord", Map.of("chat", "${message}", "emote", "_${message}_"),
                    "game", Map.of("chat", "${display_name}: ${message}")
                )
            )
        );
    }
}
