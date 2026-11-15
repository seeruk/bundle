package dev.seeruk.bundle.discord;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @param botToken  The bot's token, which it logs in with.
 * @param channelId The ID of the channel the bot should bridge chat with.
 * @param formats   How players' messages are written in Discord.
 */
public record DiscordConfig(String botToken, String channelId, Formats formats) {
    // Discord IDs are numbers, which YAML reads as numbers unless they're quoted, so either is fine
    private static final Codec<String> ID_CODEC = Codec.withAlternative(Codec.STRING, Codec.LONG, String::valueOf)
        .validate(id -> id.isEmpty() || id.chars().allMatch(Character::isDigit)
            ? DataResult.success(id)
            : DataResult.error(() -> "Not a Discord ID (copy it from Discord): " + id));

    public static final Codec<DiscordConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("bot_token").forGetter(DiscordConfig::botToken),
        ID_CODEC.fieldOf("channel_id").forGetter(DiscordConfig::channelId),
        Formats.CODEC.fieldOf("formats").forGetter(DiscordConfig::formats)
    ).apply(instance, DiscordConfig::new));

    /**
     * Each format can use {@code ${display_name}} for the player's name, as it's shown in chat, and
     * {@code ${message}} for their message.
     *
     * @param chat  How chat messages are written.
     * @param emote How emotes (i.e. {@code /me}) are written.
     */
    public record Formats(DiscordTemplate chat, DiscordTemplate emote) {
        public static final Codec<Formats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DiscordTemplate.CODEC.fieldOf("chat").forGetter(Formats::chat),
            DiscordTemplate.CODEC.fieldOf("emote").forGetter(Formats::emote)
        ).apply(instance, Formats::new));
    }

    /**
     * Hides the token, so it can't end up in logs or crash reports.
     */
    @Override
    public String toString() {
        return "DiscordConfig[botToken=" + (botToken.isBlank() ? "" : "<hidden>")
            + ", channelId=" + channelId
            + ", formats=" + formats + "]";
    }
}
