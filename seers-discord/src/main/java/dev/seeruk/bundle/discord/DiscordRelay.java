package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.markdown.MarkdownRenderer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Relays what happens in game to Discord, e.g. players' chat, which is sent as "the player".
 */
public final class DiscordRelay {
    // Discord refuses webhook names longer than this, or with these words in them
    private static final int MAX_NAME_LENGTH = 80;
    private static final Pattern RESERVED_WORD = Pattern.compile("(?i)discord|clyde");

    private DiscordRelay() {}

    /**
     * @param message The message as it was sent, after any formatting.
     * @param chat    The kind of message (only chat and emotes are relayed), and who sent it.
     * @param sender  The player who sent it.
     * @param formats How messages are written in Discord.
     */
    public static Optional<WebhookMessage> message(
        PlayerChatMessage message,
        ChatType.Bound chat,
        ServerPlayer sender,
        DiscordConfig.DiscordFormats formats
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
            "display_name", MarkdownRenderer.escape(displayName),
            "message", MarkdownRenderer.render(message.decoratedContent())
        ));

        return Optional.of(new WebhookMessage(
            username(displayName),
            avatarUrl(sender.getUUID()),
            content,
            ItemEmbeds.of(message.decoratedContent(), sender)
        ));
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
