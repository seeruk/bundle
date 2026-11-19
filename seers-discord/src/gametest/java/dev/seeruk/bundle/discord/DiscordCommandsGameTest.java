package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.config.CommandsConfig;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class DiscordCommandsGameTest {
    private static final CommandsConfig ON = new CommandsConfig(
        false,
        new CommandsConfig.Command(true),
        new CommandsConfig.Command(true)
    );

    @GameTest
    public void playersAreListedAlphabeticallyWithAfkPlayersLast(GameTestHelper helper) {
        final var alex = named(helper, "alex");
        final var blake = named(helper, "blake");
        final var casey = named(helper, "casey");

        // Blake's AFK, so they're listed after Casey
        final var embed = DiscordCommands.players(List.of(casey, blake, alex), 20, player -> player == blake);

        helper.assertValueEqual(embed.getTitle(), "3 of 20 players online", "title");
        helper.assertValueEqual(
            embed.getDescription(),
            String.join("\n", "- " + name(alex), "- " + name(casey), "- " + name(blake) + " [AFK]"),
            "players"
        );
        helper.succeed();
    }

    @GameTest
    public void playersSaysWhenNobodyIsOnline(GameTestHelper helper) {
        final var embed = DiscordCommands.players(List.of(), 20, player -> false);

        helper.assertValueEqual(embed.getTitle(), "0 of 20 players online", "title");
        helper.assertValueEqual(embed.getDescription(), "Nobody's online", "players");
        helper.succeed();
    }

    @GameTest
    public void statusShowsUptimePlayersAndPerformance(GameTestHelper helper) {
        final var embed = DiscordCommands.status(3, 20, Duration.ofHours(2).plusMinutes(5), 20);

        helper.assertValueEqual(embed.getTitle(), "The server is online", "title");
        helper.assertValueEqual(field(embed, "Uptime"), "2 hours, 5 minutes", "uptime");
        helper.assertValueEqual(field(embed, "Players"), "3 of 20", "players");
        // The GameTest server's been ticking, so there are averages for each window
        helper.assertValueEqual(field(embed, "TPS (1m, 5m, 15m)").split(" / ").length, 3, "TPS averages");
        helper.assertFalse(field(embed, "MSPT (1m, 5m, 15m)").contains("-"), "Should have MSPT averages");
        // The GameTest server ticks as fast as it can, so this checks TPS is shown at most at the
        // target rate
        for (final var tps : field(embed, "TPS (1m, 5m, 15m)").split(" / ")) {
            helper.assertTrue(Double.parseDouble(tps) <= 20, "TPS should be at most 20: " + tps);
        }
        helper.succeed();
    }

    @GameTest
    public void commandsThatAreTurnedOffAreUnavailable(GameTestHelper helper) {
        final var off = new CommandsConfig(false, new CommandsConfig.Command(false), new CommandsConfig.Command(true));

        final var embed = DiscordCommands.reply(
            DiscordCommands.PLAYERS,
            helper.getLevel().getServer(),
            Duration.ZERO,
            player -> false,
            off
        );

        helper.assertValueEqual(embed.getDescription(), "That command isn't available.", "reply");
        helper.succeed();
    }

    @GameTest
    public void playerListIsTheSameAsPlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();

        final var players = DiscordCommands.reply(DiscordCommands.PLAYERS, server, Duration.ZERO, player -> false, ON);
        final var playerList = DiscordCommands.reply(
            DiscordCommands.PLAYER_LIST,
            server,
            Duration.ZERO,
            player -> false,
            ON
        );

        helper.assertValueEqual(playerList.getTitle(), players.getTitle(), "title");
        helper.succeed();
    }

    private static ServerPlayer named(GameTestHelper helper, String name) {
        // Unique, as players share a server, but starting with the name, so they sort by it
        final var unique = name + "_" + UUID.randomUUID().toString().substring(0, 4);
        return player(helper, new NameAndId(UUID.randomUUID(), unique));
    }

    private static String name(ServerPlayer player) {
        return player.getDisplayName().getString().replace("_", "\\_");
    }

    private static String field(MessageEmbed embed, String name) {
        return embed.getFields().stream()
            .filter(field -> name.equals(field.getName()))
            .findFirst()
            .orElseThrow()
            .getValue();
    }
}
