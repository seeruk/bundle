package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.config.CommandsConfig;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiscordCommandsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> shouldShowUptimeInItsTwoLargestUnits() {
        return Stream.of(
            Arguments.of("Shows days and hours", Duration.ofDays(2).plusHours(4).plusMinutes(30), "2 days, 4 hours"),
            Arguments.of("Shows hours and minutes", Duration.ofHours(1).plusMinutes(5), "1 hour, 5 minutes"),
            Arguments.of("Shows one unit when the next is zero", Duration.ofDays(1).plusMinutes(5), "1 day"),
            Arguments.of("Shows seconds", Duration.ofSeconds(42), "42 seconds"),
            Arguments.of("Shows no time", Duration.ZERO, "0 seconds")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldShowUptimeInItsTwoLargestUnits(final String description, final Duration uptime, final String expected) {
        assertEquals(expected, DiscordCommands.uptime(uptime));
    }

    static Stream<Arguments> shouldOnlyAddCommandsThatAreTurnedOn() {
        final var on = new CommandsConfig.Command(true);
        final var off = new CommandsConfig.Command(false);
        return Stream.of(
            Arguments.of("Adds every command", on, on, List.of("players", "playerlist", "status")),
            Arguments.of("Leaves out /players and /playerlist", off, on, List.of("status")),
            Arguments.of("Leaves out /status", on, off, List.of("players", "playerlist"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldOnlyAddCommandsThatAreTurnedOn(
        final String description,
        final CommandsConfig.Command players,
        final CommandsConfig.Command status,
        final List<String> expected
    ) {
        final var commands = DiscordCommands.enabled(new CommandsConfig(false, players, status));

        assertEquals(expected, commands.stream().map(CommandData::getName).toList());
    }
}
