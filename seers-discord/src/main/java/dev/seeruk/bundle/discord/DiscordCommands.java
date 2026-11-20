package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.tick.TickStats;
import dev.seeruk.bundle.discord.config.CommandsConfig;
import dev.seeruk.bundle.discord.markdown.MarkdownRenderer;
import dev.seeruk.bundle.text.TextComponents;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handlers for the bot's slash commands in Discord.
 */
public final class DiscordCommands {
    // Command names
    static final String PLAYERS = "players";
    static final String PLAYER_LIST = "playerlist";
    static final String STATUS = "status";

    // Replies are about the server, which is up, so they're green
    private static final int COLOUR = TextColor.fromLegacyFormat(ChatFormatting.GREEN).getValue();

    private DiscordCommands() {}

    /**
     * Returns a list of commands containing only those that are enabled.
     */
    static List<CommandData> enabled(CommandsConfig config) {
        final var commands = new ArrayList<CommandData>();
        if (config.players().enabled()) {
            commands.add(Commands.slash(PLAYERS, "Shows who's online"));
            commands.add(Commands.slash(PLAYER_LIST, "Shows who's online"));
        }
        if (config.status().enabled()) {
            commands.add(Commands.slash(STATUS, "Shows how the server's doing"));
        }
        return commands;
    }

    /**
     * Returns the reply to a command, from the server's current state, so it needs to be called
     * on the server thread.
     *
     * @param command The command's name.
     * @param server  The server.
     * @param uptime  How long the server's been running.
     * @param isAfk   A function to check whether a player is AFK.
     * @param config  Which commands are turned on.
     */
    static MessageEmbed reply(
        String command,
        MinecraftServer server,
        Duration uptime,
        Predicate<ServerPlayer> isAfk,
        CommandsConfig config
    ) {
        final var players = server.getPlayerList().getPlayers();
        final var max = server.getPlayerList().getMaxPlayers();

        return switch (command) {
            case PLAYERS, PLAYER_LIST -> config.players().enabled()
                ? players(players, max, isAfk)
                : unavailable();
            case STATUS -> config.status().enabled()
                ? status(players.size(), max, uptime, server.tickRateManager().tickrate())
                : unavailable();
            default -> unavailable();
        };
    }

    /**
     * Returns who's online, as a bulleted list of their names as chat shows them, in alphabetical
     * order. AFK players are listed after everyone else, as they are in the tab list.
     */
    static MessageEmbed players(List<ServerPlayer> players, int max, Predicate<ServerPlayer> isAfk) {
        final Function<ServerPlayer, String> name = player -> player.getDisplayName().getString();
        final var byAfk = players.stream().collect(Collectors.partitioningBy(isAfk));

        final var lines = Stream.concat(
                // Non-AFK players
                byAfk.get(false).stream().map(name).sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(MarkdownRenderer::escape),
                // AFK players
                byAfk.get(true).stream().map(name).sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(afk -> MarkdownRenderer.escape(afk) + " [AFK]")
            )
            // A list in Discord's Markdown, which it shows with bullets
            .map(line -> "- " + line)
            .toList();

        return new EmbedBuilder()
            .setTitle(players.size() + " of " + max + " players online")
            .setDescription(lines.isEmpty()
                ? "Nobody's online"
                : TextComponents.truncate(String.join("\n", lines), MessageEmbed.DESCRIPTION_MAX_LENGTH))
            .setColor(COLOUR)
            .build();
    }

    /**
     * Returns the current status of the server (i.e. an indication of its health, and high-level
     * basic information.
     *
     * @param players  How many players are online.
     * @param max      How many players can be online.
     * @param uptime   How long the server's been running.
     * @param tickRate How many ticks a second the server's aiming for, usually 20.
     */
    static MessageEmbed status(int players, int max, Duration uptime, float tickRate) {
        // The server ticks faster than its rate for a moment when it's catching up (e.g. just after
        // starting), but it's only ever aiming for its rate, so that's shown at most
        final Function<TickStats.Window, Double> tps = window -> Math.min(TickStats.ticksPerSecond(window), tickRate);

        return new EmbedBuilder()
            .setTitle("The server is online")
            .addField("Uptime", uptime(uptime), true)
            .addField("Players", players + " of " + max, true)
            .addField("Version", SharedConstants.getCurrentVersion().name(), true)
            .addField("TPS (1m, 5m, 15m)", averages(tps), true)
            .addField("MSPT (1m, 5m, 15m)", averages(TickStats::millisecondsPerTick), true)
            .setColor(COLOUR)
            .build();
    }

    /**
     * Returns how long something's taken, in its two largest units, e.g. "2 days, 4 hours", or
     * "5 minutes, 1 second".
     */
    static String uptime(Duration uptime) {
        final var units = List.of(
            unit(uptime.toDaysPart(), "day"),
            unit(uptime.toHoursPart(), "hour"),
            unit(uptime.toMinutesPart(), "minute"),
            unit(uptime.toSecondsPart(), "second")
        );

        // Skips leading units that are zero, so it starts with the largest that isn't
        final var shown = units.stream()
            .dropWhile(String::isEmpty)
            .limit(2)
            .filter(unit -> !unit.isEmpty())
            .toList();
        return shown.isEmpty() ? "0 seconds" : String.join(", ", shown);
    }

    private static String unit(long count, String name) {
        return count == 0 ? "" : count + " " + name + (count == 1 ? "" : "s");
    }

    private static String averages(Function<TickStats.Window, Double> average) {
        return Stream.of(TickStats.Window.values())
            .map(average)
            // There's no average until the server's ticked a couple of times
            .map(value -> value.isNaN() ? "-" : String.format(Locale.ROOT, "%.1f", value))
            .collect(Collectors.joining(" / "));
    }

    /**
     * Returns the reply to any command while the server's stopping, when it can't answer them.
     */
    static MessageEmbed stopping() {
        return new EmbedBuilder().setDescription("The server is stopping.").build();
    }

    private static MessageEmbed unavailable() {
        return new EmbedBuilder().setDescription("That command isn't available.").build();
    }
}
