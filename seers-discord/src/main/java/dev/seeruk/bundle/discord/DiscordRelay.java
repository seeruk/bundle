package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.config.DiscordConfig;
import dev.seeruk.bundle.discord.config.EventsConfig;
import dev.seeruk.bundle.discord.markdown.MarkdownRenderer;
import dev.seeruk.bundle.text.TextComponents;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Relays what happens in game to Discord, e.g. players' chat, which is sent as "the player", and
 * events like players joining, which are sent as the webhook itself.
 */
public final class DiscordRelay {
    private DiscordRelay() {}

    /**
     * @param message The message as it was sent, after any formatting.
     * @param chat    The kind of message (only chat and emotes are relayed), and who sent it.
     * @param sender  The player who sent it.
     * @param settings How chat is written in Discord.
     */
    public static Optional<WebhookMessage> message(
        PlayerChatMessage message,
        ChatType.Bound chat,
        ServerPlayer sender,
        DiscordConfig.DiscordSettings settings
    ) {
        final DiscordTemplate format;
        if (chat.chatType().is(ChatType.CHAT)) {
            format = settings.chat();
        } else if (chat.chatType().is(ChatType.EMOTE_COMMAND)) {
            format = settings.emote();
        } else {
            return Optional.empty();
        }

        // The name chat shows, e.g. with the player's nickname, or their team's prefix
        final var displayName = chat.name().getString();
        // Tags (e.g. a long item name, many times over) can make a message too long for Discord
        final var content = TextComponents.truncate(
            format.format(Map.of(
                "display_name", MarkdownRenderer.escape(displayName),
                "message", MarkdownRenderer.render(message.decoratedContent())
            )),
            Message.MAX_CONTENT_LENGTH
        );

        // Discord refuses a message with nothing in it, e.g. if the format's been emptied to stop
        // chat being sent
        final var embeds = ItemEmbeds.of(message.decoratedContent(), sender);
        if (content.isBlank() && embeds.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new WebhookMessage(
            WebhookNames.allowed(displayName),
            avatarUrl(sender.getUUID()),
            content,
            embeds
        ));
    }

    /**
     * Returns a message for when a player joins.
     */
    public static Optional<WebhookMessage> joined(ServerPlayer player, EventsConfig events) {
        return event(events.join(), player, Map.of());
    }

    /**
     * Returns a message for when a player leaves.
     */
    public static Optional<WebhookMessage> left(ServerPlayer player, EventsConfig events) {
        return event(events.quit(), player, Map.of());
    }

    /**
     * Returns a message for when a player dies.
     *
     * @param player  The player who died.
     * @param message Their death message, as the game shows it.
     * @param events  Which events are sent, and how they look.
     */
    public static Optional<WebhookMessage> died(ServerPlayer player, Component message, EventsConfig events) {
        // A player's team can hide their death message from players outside it (or everyone), in
        // which case it's not sent to Discord either, where anyone could see it
        final var team = player.getTeam();
        if (team != null && !showsDeathsToOthers(team.getDeathMessageVisibility())) {
            return Optional.empty();
        }

        return event(events.death(), player, Map.of("message", message.getString()));
    }

    /**
     * Returns a message for when a player goes AFK, or comes back.
     *
     * @param player The player.
     * @param isAfk  Whether they're AFK now.
     * @param events Which events are sent, and how they look.
     */
    public static Optional<WebhookMessage> afk(ServerPlayer player, boolean isAfk, EventsConfig events) {
        return event(isAfk ? events.afk() : events.afkReturn(), player, Map.of());
    }

    /**
     * Returns a message for when resource zones are reset.
     *
     * @param chunks Each dimension's reset chunks.
     * @param events Which events are sent, and how they look.
     */
    public static Optional<WebhookMessage> resourceZonesReset(
        Map<ResourceKey<Level>, Set<ChunkPos>> chunks,
        EventsConfig events
    ) {
        final var counts = new HashMap<ResourceKey<Level>, Integer>();
        chunks.forEach((dimension, reset) -> counts.put(dimension, reset.size()));

        return event(
            events.resourceZonesReset(), null, Map.of(
                "chunk_count", String.valueOf(counts.values().stream().mapToInt(Integer::intValue).sum()),
                "summary", resetSummary(counts)
            )
        );
    }

    /**
     * How many chunks were reset in each dimension, e.g. "120 chunks in Overworld, 1 chunk in The
     * Nether", in order of the dimensions' IDs.
     */
    static String resetSummary(Map<ResourceKey<Level>, Integer> counts) {
        return counts.entrySet().stream()
            .filter(count -> count.getValue() > 0)
            .sorted(Comparator.comparing(count -> count.getKey().identifier()))
            .map(count -> "%d %s in %s".formatted(
                count.getValue(),
                count.getValue() == 1 ? "chunk" : "chunks",
                dimensionName(count.getKey())
            ))
            .collect(Collectors.joining(", "));
    }

    /**
     * A dimension's name, from its ID's path in title case, e.g. "The Nether".
     */
    private static String dimensionName(ResourceKey<Level> dimension) {
        return Arrays.stream(dimension.identifier().getPath().split("[_/]"))
            .filter(word -> !word.isEmpty())
            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
            .collect(Collectors.joining(" "));
    }

    /**
     * A category's winner in one of Seer's Leaders' periods, as it's shown in Discord.
     *
     * @param category    The category's title.
     * @param displayName The winner's name, as chat shows it (e.g. their nickname).
     * @param username    The winner's username.
     * @param value       Their total, in the category's unit.
     */
    public record LeadersWinner(String category, String displayName, String username, String value) {}

    /**
     * Returns the embed for a category in Seer's Leaders getting a new leader. It's only an embed,
     * not a whole message, as new leaders found at once are sent together.
     *
     * @param leader              The new leader, whose face is shown.
     * @param displayName         Their name, as chat shows it (e.g. their nickname).
     * @param username            Their username.
     * @param previousDisplayName Who led the category before, as chat shows them.
     * @param category            The category's title.
     * @param events              Which events are sent, and how they look.
     */
    public static Optional<MessageEmbed> leadersChanged(
        UUID leader,
        String displayName,
        String username,
        String previousDisplayName,
        String category,
        EventsConfig events
    ) {
        final var format = events.leadersChanged();
        if (!format.enabled()) {
            return Optional.empty();
        }

        return embed(
            format.format(), format.colour(), leader, Map.of(
                "display_name", displayName,
                "username", username,
                "previous_display_name", previousDisplayName,
                "category", category
            )
        ).map(EmbedBuilder::build);
    }

    /**
     * Returns the messages for a period in Seer's Leaders ending: an embed with a line for each
     * winner, continued in more embeds (and messages) if there are more than fit in one.
     *
     * @param period  The period's title.
     * @param winners Who won each of the period's categories.
     * @param events  Which events are sent, and how they look.
     */
    public static List<WebhookMessage> leadersPeriodEnded(String period, List<LeadersWinner> winners, EventsConfig events) {
        final var format = events.leadersPeriodEnded();
        if (!format.enabled()) {
            return List.of();
        }

        // The lines can use Discord's formatting, so values in them are escaped
        final var lines = winners.stream()
            .map(winner -> TextComponents.truncate(
                format.lineFormat().format(Map.of(
                    "category", MarkdownRenderer.escape(winner.category()),
                    "display_name", MarkdownRenderer.escape(winner.displayName()),
                    "username", MarkdownRenderer.escape(winner.username()),
                    "value", MarkdownRenderer.escape(winner.value())
                )),
                MessageEmbed.DESCRIPTION_MAX_LENGTH
            ))
            .filter(line -> !line.isBlank())
            .toList();

        final var embeds = new ArrayList<MessageEmbed>();
        for (final var description : descriptions(lines)) {
            final var line = embeds.isEmpty() ? format.format() : format.continuedFormat();
            embed(line, format.colour(), null, Map.of("period", period))
                .ifPresent(embed -> embeds.add(embed.setDescription(description).build()));
        }

        return WebhookMessage.packed(embeds);
    }

    /**
     * Joins the lines into as few embed descriptions as they fit in, in order. With no lines,
     * there's still one (empty) description, so the embed's top line is still sent.
     */
    static List<String> descriptions(List<String> lines) {
        final var descriptions = new ArrayList<String>();
        var current = new StringBuilder();

        for (final var line : lines) {
            if (!current.isEmpty() && current.length() + 1 + line.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                descriptions.add(current.toString());
                current = new StringBuilder();
            }

            current.append(current.isEmpty() ? "" : "\n").append(line);
        }

        if (!current.isEmpty() || descriptions.isEmpty()) {
            descriptions.add(current.toString());
        }
        return descriptions;
    }

    /**
     * Returns a message for when the server starts.
     */
    public static Optional<WebhookMessage> serverStarted(EventsConfig events) {
        return event(events.serverStart(), null, Map.of());
    }

    /**
     * Returns a message for when the server stops.
     */
    public static Optional<WebhookMessage> serverStopped(EventsConfig events) {
        return event(events.serverStop(), null, Map.of());
    }

    /**
     * Whether a team's death messages are shown to players outside it.
     */
    private static boolean showsDeathsToOthers(Team.Visibility visibility) {
        return visibility == Team.Visibility.ALWAYS || visibility == Team.Visibility.HIDE_FOR_OWN_TEAM;
    }

    /**
     * Returns the embed for a player completing an advancement. It's only an embed, not a whole
     * message, as advancements completed at once are sent together (see
     * {@link AdvancementBatcher}).
     *
     * @param player      The player who completed it.
     * @param advancement The advancement, which needs a display, as announced ones do.
     * @param message     Its announcement, as the game shows it.
     * @param events      Which events are sent, and how they look.
     */
    public static Optional<MessageEmbed> advancement(
        ServerPlayer player,
        AdvancementHolder advancement,
        Component message,
        EventsConfig events
    ) {
        final var format = events.advancement();
        final var display = advancement.value().display();
        if (!format.enabled() || display.isEmpty()) {
            return Optional.empty();
        }

        final var values = Map.of(
            "message", message.getString(),
            "advancement", display.get().title().getString(),
            "description", display.get().description().getString()
        );

        final var colour = display.get().type() == AdvancementType.CHALLENGE
            ? format.challengeColour()
            : format.colour();

        // The main text can use Discord's formatting, so values in it are escaped
        final var escaped = new HashMap<String, String>();
        values.forEach((key, value) -> escaped.put(key, MarkdownRenderer.escape(value)));
        playerValues(player).forEach((key, value) -> escaped.put(key, MarkdownRenderer.escape(value)));

        final var description = TextComponents.truncate(
            format.descriptionFormat().format(escaped),
            MessageEmbed.DESCRIPTION_MAX_LENGTH
        );

        return embed(format.format(), colour, player.getUUID(), withPlayerValues(player, values))
            .map(embed -> embed.setDescription(description).build());
    }

    /**
     * Returns the embed sent instead of each advancement's, when a player completes more at once
     * than fit in one message (e.g. with a command).
     *
     * @param player The player who completed them.
     * @param count  How many they completed.
     * @param events Which events are sent, and how they look.
     */
    public static Optional<MessageEmbed> advancementSummary(ServerPlayer player, int count, EventsConfig events) {
        final var format = events.advancement();
        if (!format.enabled()) {
            return Optional.empty();
        }

        return embed(
            format.summaryFormat(),
            format.colour(),
            player.getUUID(),
            withPlayerValues(player, Map.of("count", String.valueOf(count)))
        ).map(EmbedBuilder::build);
    }

    /**
     * Returns a message for a general player event (i.e. a "simple" event).
     *
     * @param event  How the event looks.
     * @param player The player it's about, whose face is shown, or null if it's not about a player.
     * @param values Values for the event's placeholders, besides the player's.
     */
    private static Optional<WebhookMessage> event(
        EventsConfig.EventFormat event,
        ServerPlayer player,
        Map<String, String> values
    ) {
        if (!event.enabled()) {
            return Optional.empty();
        }

        return embed(
            event.format(),
            event.colour(),
            player == null ? null : player.getUUID(),
            withPlayerValues(player, values)
        )
            .map(embed -> WebhookMessage.embed(embed.build()));
    }

    /**
     * Returns an event's embed, with the format filled in as the line at the top, next to a
     * player's face, or nothing if that line would be empty.
     *
     * @param format The line at the top.
     * @param colour The embed's colour.
     * @param face   The player whose face is shown, or null if it's not about a player.
     * @param values Values for the format's placeholders.
     */
    private static Optional<EmbedBuilder> embed(
        DiscordTemplate format,
        TextColor colour,
        UUID face,
        Map<String, String> values
    ) {
        // Discord doesn't show formatting in an embed's author line, so nothing's escaped
        final var line = format.format(values);
        if (line.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new EmbedBuilder()
            .setAuthor(
                TextComponents.truncate(line, MessageEmbed.AUTHOR_MAX_LENGTH),
                null,
                face == null ? null : avatarUrl(face)
            )
            .setColor(colour.getValue()));
    }

    /**
     * Returns the values, with those for the placeholders every player event has, if there's a
     * player.
     */
    private static Map<String, String> withPlayerValues(ServerPlayer player, Map<String, String> values) {
        final var all = new HashMap<>(values);
        if (player != null) {
            all.putAll(playerValues(player));
        }
        return all;
    }

    /**
     * Returns the values for the placeholders every player event has.
     */
    private static Map<String, String> playerValues(ServerPlayer player) {
        return Map.of(
            // The name chat shows, e.g. with the player's nickname, or their team's prefix
            "display_name", player.getDisplayName().getString(),
            "username", player.getGameProfile().name()
        );
    }

    static String avatarUrl(UUID id) {
        return "https://api.mineatar.io/head/" + id.toString().replace("-", "") + "?scale=16";
    }
}
