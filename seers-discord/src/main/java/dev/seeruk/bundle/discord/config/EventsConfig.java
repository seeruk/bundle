package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.discord.DiscordTemplate;
import net.minecraft.network.chat.TextColor;

/**
 * Configuration for the events from the game that are sent to Discord, and how they look there.
 *
 * <p>The first line of each event's embed is the {@code format}. Discord doesn't show formatting
 * in the title, so it's plain text with support for some placeholders. Player events can use
 * {@code ${display_name}} and {@code ${username}}. Colours (in {@code colour}) are either Minecraft
 * colour names (e.g. {@code yellow}), or hex (e.g. {@code #FFAA00}).
 *
 * @param join               A player joining.
 * @param quit               A player leaving.
 * @param death              A player dying. Its formats can use {@code ${message}} for the game's
 *                           death message.
 * @param advancement        A player completing an advancement the game announces in chat.
 * @param afk                A player going AFK.
 * @param afkReturn          A player coming back from being AFK.
 * @param resourceZonesReset Resource zones being reset. Its format can use {@code ${chunk_count}}
 *                           for how many chunks were reset, and {@code ${summary}} for how many in
 *                           each dimension.
 * @param leadersChanged     A category in Seer's Leaders getting a new leader. Its format can use
 *                           {@code ${category}} for the category's title, and
 *                           {@code ${previous_display_name}} for who led it before.
 * @param leadersPeriodEnded A period in Seer's Leaders ending, with its winners.
 * @param serverStart        The server starting.
 * @param serverStop         The server stopping.
 */
public record EventsConfig(
    EventFormat join,
    EventFormat quit,
    EventFormat death,
    AdvancementFormat advancement,
    EventFormat afk,
    EventFormat afkReturn,
    EventFormat resourceZonesReset,
    EventFormat leadersChanged,
    LeadersPeriodFormat leadersPeriodEnded,
    EventFormat serverStart,
    EventFormat serverStop
) {
    public static final Codec<EventsConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        EventFormat.CODEC.fieldOf("join").forGetter(EventsConfig::join),
        EventFormat.CODEC.fieldOf("quit").forGetter(EventsConfig::quit),
        EventFormat.CODEC.fieldOf("death").forGetter(EventsConfig::death),
        AdvancementFormat.CODEC.fieldOf("advancement").forGetter(EventsConfig::advancement),
        EventFormat.CODEC.fieldOf("afk").forGetter(EventsConfig::afk),
        EventFormat.CODEC.fieldOf("afk_return").forGetter(EventsConfig::afkReturn),
        EventFormat.CODEC.fieldOf("resource_zones_reset").forGetter(EventsConfig::resourceZonesReset),
        EventFormat.CODEC.fieldOf("leaders_changed").forGetter(EventsConfig::leadersChanged),
        LeadersPeriodFormat.CODEC.fieldOf("leaders_period_ended").forGetter(EventsConfig::leadersPeriodEnded),
        EventFormat.CODEC.fieldOf("server_start").forGetter(EventsConfig::serverStart),
        EventFormat.CODEC.fieldOf("server_stop").forGetter(EventsConfig::serverStop)
    ).apply(instance, EventsConfig::new));

    /**
     * @param enabled Whether the event is sent to Discord.
     * @param format  The line at the top of the event's embed.
     * @param colour  The embed's colour.
     */
    public record EventFormat(boolean enabled, DiscordTemplate format, TextColor colour) {
        public static final Codec<EventFormat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(EventFormat::enabled),
            DiscordTemplate.CODEC.fieldOf("format").forGetter(EventFormat::format),
            TextColor.CODEC.fieldOf("colour").forGetter(EventFormat::colour)
        ).apply(instance, EventFormat::new));
    }

    /**
     * Advancements' formats can use {@code ${message}} for the game's announcement (e.g. "Seer has
     * made the advancement [Stone Age]"), {@code ${advancement}} for its title, and
     * {@code ${description}} for its description.
     *
     * @param enabled           Whether advancements are sent to Discord.
     * @param format            The line at the top of the embed.
     * @param descriptionFormat The embed's main text, which can use Discord's formatting, or
     *                          nothing if it's empty.
     * @param summaryFormat     The line at the top of the embed sent instead, when a player
     *                          completes more advancements at once than fit in one message (e.g.
     *                          with a command). It can use {@code ${count}} for how many.
     * @param colour            The embed's colour for tasks and goals, and summaries.
     * @param challengeColour   The embed's colour for challenges.
     */
    public record AdvancementFormat(
        boolean enabled,
        DiscordTemplate format,
        DiscordTemplate descriptionFormat,
        DiscordTemplate summaryFormat,
        TextColor colour,
        TextColor challengeColour
    ) {
        public static final Codec<AdvancementFormat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(AdvancementFormat::enabled),
            DiscordTemplate.CODEC.fieldOf("format").forGetter(AdvancementFormat::format),
            DiscordTemplate.CODEC.fieldOf("description_format").forGetter(AdvancementFormat::descriptionFormat),
            DiscordTemplate.CODEC.fieldOf("summary_format").forGetter(AdvancementFormat::summaryFormat),
            TextColor.CODEC.fieldOf("colour").forGetter(AdvancementFormat::colour),
            TextColor.CODEC.fieldOf("challenge_colour").forGetter(AdvancementFormat::challengeColour)
        ).apply(instance, AdvancementFormat::new));
    }

    /**
     * Seer's Leaders' period results are one embed, with a line for each category's winner. Its
     * formats can use {@code ${period}} for the period's title, and its lines {@code ${category}}
     * for the category's title, {@code ${display_name}} and {@code ${username}} for the winner, and
     * {@code ${value}} for their total.
     *
     * @param enabled         Whether period results are sent to Discord.
     * @param format          The line at the top of the embed.
     * @param continuedFormat The line at the top of any further embeds, when there are more winners
     *                        than fit in one.
     * @param lineFormat      Each winner's line in the embed's main text, which can use Discord's
     *                        formatting.
     * @param colour          The embed's colour.
     */
    public record LeadersPeriodFormat(
        boolean enabled,
        DiscordTemplate format,
        DiscordTemplate continuedFormat,
        DiscordTemplate lineFormat,
        TextColor colour
    ) {
        public static final Codec<LeadersPeriodFormat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(LeadersPeriodFormat::enabled),
            DiscordTemplate.CODEC.fieldOf("format").forGetter(LeadersPeriodFormat::format),
            DiscordTemplate.CODEC.fieldOf("continued_format").forGetter(LeadersPeriodFormat::continuedFormat),
            DiscordTemplate.CODEC.fieldOf("line_format").forGetter(LeadersPeriodFormat::lineFormat),
            TextColor.CODEC.fieldOf("colour").forGetter(LeadersPeriodFormat::colour)
        ).apply(instance, LeadersPeriodFormat::new));
    }
}
