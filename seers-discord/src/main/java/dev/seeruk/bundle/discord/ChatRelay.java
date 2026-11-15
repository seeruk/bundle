package dev.seeruk.bundle.discord;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Relays in-game chat from players into messages for Discord, sent as the player.
 */
public final class ChatRelay {
    // Discord refuses webhook names longer than this, or with these words in them
    private static final int MAX_NAME_LENGTH = 80;
    private static final Pattern RESERVED_WORD = Pattern.compile("(?i)discord|clyde");

    private ChatRelay() {}

    /**
     * @param message The message as it was sent, after any formatting.
     * @param chat    The kind of message (only chat messages and emotes are relayed), and who sent it.
     * @param id      The sending player's UUID.
     * @param formats How messages are written in Discord.
     */
    public static Optional<WebhookMessage> message(
        PlayerChatMessage message,
        ChatType.Bound chat,
        UUID id,
        DiscordConfig.Formats formats
    ) {
        final DiscordTemplate format;
        if (chat.chatType().is(ChatType.CHAT)) {
            format = formats.chat();
        } else if (chat.chatType().is(ChatType.EMOTE_COMMAND)) {
            format = formats.emote();
        } else {
            return Optional.empty();
        }

        // The name chat shows, e.g. with the player's nickname, or their team's prefix
        final var displayName = chat.name().getString();
        final var content = format.format(Map.of(
            "display_name", DiscordMarkdown.escape(displayName),
            "message", DiscordMarkdown.of(message.decoratedContent())
        ));

        return Optional.of(new WebhookMessage(username(displayName), avatarUrl(id), content));
    }

    /**
     * The name as Discord will allow it as a webhook's name. Reserved words are visibly censored
     * (e.g. {@code D*****d}), so it's clear the name's been changed.
     */
    static String username(String name) {
        final var allowed = RESERVED_WORD.matcher(name).replaceAll(word -> {
            final var text = word.group();
            return text.charAt(0) + "*".repeat(text.length() - 2) + text.charAt(text.length() - 1);
        });
        if (allowed.codePointCount(0, allowed.length()) <= MAX_NAME_LENGTH) {
            return allowed;
        }

        return allowed.substring(0, allowed.offsetByCodePoints(0, MAX_NAME_LENGTH));
    }

    static String avatarUrl(UUID id) {
        return "https://api.mineatar.io/head/" + id.toString().replace("-", "") + "?scale=16";
    }
}
