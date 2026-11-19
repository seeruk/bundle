package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.discord.DiscordTemplate;
import dev.seeruk.bundle.text.TextTemplate;

/**
 * @param botToken  The bot's token, which it logs in with.
 * @param channelId The ID of the channel the bot should bridge chat with.
 * @param discord   Everything in Discord, i.e. how chat and events from the game look there, and the
 *                  bot's commands.
 * @param game      How everything shown in game from Discord looks, i.e. chat from Discord.
 */
public record DiscordConfig(String botToken, String channelId, DiscordSettings discord, GameSettings game) {
    // Discord IDs are numbers, which YAML reads as numbers unless they're quoted, so either is fine
    private static final Codec<String> ID_CODEC = Codec.withAlternative(Codec.STRING, Codec.LONG, String::valueOf)
        .validate(id -> id.isEmpty() || isId(id)
            ? DataResult.success(id)
            : DataResult.error(() -> "Not a Discord ID (copy it from Discord): " + id));

    public static final Codec<DiscordConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("bot_token").forGetter(DiscordConfig::botToken),
        ID_CODEC.fieldOf("channel_id").forGetter(DiscordConfig::channelId),
        DiscordSettings.CODEC.fieldOf("discord").forGetter(DiscordConfig::discord),
        GameSettings.CODEC.fieldOf("game").forGetter(DiscordConfig::game)
    ).apply(instance, DiscordConfig::new));

    /**
     * Everything in Discord: how what's sent there looks, and the bot's commands. The chat formats
     * can use {@code ${display_name}} for the player's name, as it's shown in chat, and
     * {@code ${message}} for their message.
     *
     * @param chat     How chat messages are written.
     * @param emote    How emotes (i.e. {@code /me}) are written.
     * @param events   Which events from the game are sent, and how they look.
     * @param commands Which of the bot's slash commands are available.
     */
    public record DiscordSettings(
        DiscordTemplate chat,
        DiscordTemplate emote,
        EventsConfig events,
        CommandsConfig commands
    ) {
        public static final Codec<DiscordSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DiscordTemplate.CODEC.fieldOf("chat").forGetter(DiscordSettings::chat),
            DiscordTemplate.CODEC.fieldOf("emote").forGetter(DiscordSettings::emote),
            EventsConfig.CODEC.fieldOf("events").forGetter(DiscordSettings::events),
            CommandsConfig.CODEC.fieldOf("commands").forGetter(DiscordSettings::commands)
        ).apply(instance, DiscordSettings::new));
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
    public record GameSettings(TextTemplate chat, TextTemplate reply) {
        public static final Codec<GameSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextTemplate.CODEC.fieldOf("chat").forGetter(GameSettings::chat),
            TextTemplate.CODEC.fieldOf("reply").forGetter(GameSettings::reply)
        ).apply(instance, GameSettings::new));
    }

    /**
     * Whether the text is a Discord ID, i.e. digits that fit in a {@code long}, as JDA needs them.
     */
    private static boolean isId(String id) {
        if (!id.chars().allMatch(Character::isDigit)) {
            return false;
        }

        try {
            Long.parseLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
