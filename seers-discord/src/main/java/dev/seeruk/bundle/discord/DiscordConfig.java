package dev.seeruk.bundle.discord;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.text.TextTemplate;

/**
 * @param botToken  The bot's token, which it logs in with.
 * @param channelId The ID of the channel the bot should bridge chat with.
 * @param discord   How everything sent to Discord looks there, i.e. chat from the game, and events.
 * @param game      How everything shown in game from Discord looks, i.e. chat from Discord.
 */
public record DiscordConfig(String botToken, String channelId, DiscordFormats discord, GameFormats game) {
    // Discord IDs are numbers, which YAML reads as numbers unless they're quoted, so either is fine
    private static final Codec<String> ID_CODEC = Codec.withAlternative(Codec.STRING, Codec.LONG, String::valueOf)
        .validate(id -> id.isEmpty() || id.chars().allMatch(Character::isDigit)
            ? DataResult.success(id)
            : DataResult.error(() -> "Not a Discord ID (copy it from Discord): " + id));

    public static final Codec<DiscordConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("bot_token").forGetter(DiscordConfig::botToken),
        ID_CODEC.fieldOf("channel_id").forGetter(DiscordConfig::channelId),
        DiscordFormats.CODEC.fieldOf("discord").forGetter(DiscordConfig::discord),
        GameFormats.CODEC.fieldOf("game").forGetter(DiscordConfig::game)
    ).apply(instance, DiscordConfig::new));

    /**
     * How everything sent to Discord looks there. The chat formats can use {@code ${display_name}}
     * for the player's name, as it's shown in chat, and {@code ${message}} for their message.
     *
     * @param chat   How chat messages are written.
     * @param emote  How emotes (i.e. {@code /me}) are written.
     * @param events Which events from the game are sent, and how they look.
     */
    public record DiscordFormats(DiscordTemplate chat, DiscordTemplate emote, EventsConfig events) {
        public static final Codec<DiscordFormats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DiscordTemplate.CODEC.fieldOf("chat").forGetter(DiscordFormats::chat),
            DiscordTemplate.CODEC.fieldOf("emote").forGetter(DiscordFormats::emote),
            EventsConfig.CODEC.fieldOf("events").forGetter(DiscordFormats::events)
        ).apply(instance, DiscordFormats::new));
    }

    /**
     * How everything shown in game from Discord looks. Each format can use {@code ${display_name}}
     * for the sender's name in the bot's server, {@code ${username}} for their Discord username,
     * and {@code ${message}} for their message.
     *
     * @param chat  How chat messages are shown. It can also use {@code ${reply}} for where the
     *              reply format goes, if the message is a reply.
     * @param reply How the message a reply replied to is shown, with its sender and message.
     */
    public record GameFormats(TextTemplate chat, TextTemplate reply) {
        public static final Codec<GameFormats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextTemplate.CODEC.fieldOf("chat").forGetter(GameFormats::chat),
            TextTemplate.CODEC.fieldOf("reply").forGetter(GameFormats::reply)
        ).apply(instance, GameFormats::new));
    }

    /**
     * Hides the token, so it can't end up in logs or crash reports.
     */
    @Override
    public String toString() {
        return "DiscordConfig[botToken=" + (botToken.isBlank() ? "" : "<hidden>")
            + ", channelId=" + channelId
            + ", discord=" + discord
            + ", game=" + game + "]";
    }
}
