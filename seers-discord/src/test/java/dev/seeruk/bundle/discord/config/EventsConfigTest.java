package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventsConfigTest {
    static Stream<Arguments> shouldReadColours() {
        return Stream.of(
            Arguments.of("Reads Minecraft's colour names", "yellow", 0xFFFF55),
            Arguments.of("Reads colour names with underscores", "dark_purple", 0xAA00AA),
            Arguments.of("Reads hex colours", "#FFAA00", 0xFFAA00)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldReadColours(final String description, final String colour, final int expected) {
        assertEquals(expected, parse(colour).getOrThrow().colour().getValue());
    }

    @Test
    void shouldRefuseColoursThatAreNotColours() {
        assertTrue(parse("not_a_colour").isError());
    }

    @Test
    void shouldReadLeadersPeriodFormats() {
        final var format = EventsConfig.LeadersPeriodFormat.CODEC.parse(
            JavaOps.INSTANCE,
            Map.of(
                "enabled", true,
                "format", "The ${period} leaders are in!",
                "continued_format", "The ${period} leaders are in! (cont.)",
                "line_format", "**${category}**: ${display_name} (${value})",
                "colour", "gold"
            )
        ).getOrThrow();

        assertEquals("The monthly leaders are in! (cont.)", format.continuedFormat().format(Map.of("period", "monthly")));
        assertEquals(0xFFAA00, format.colour().getValue());
    }

    private static DataResult<EventsConfig.EventFormat> parse(String colour) {
        return EventsConfig.EventFormat.CODEC.parse(
            JavaOps.INSTANCE,
            Map.of("enabled", true, "format", "${display_name} joined the game", "colour", colour)
        );
    }
}
