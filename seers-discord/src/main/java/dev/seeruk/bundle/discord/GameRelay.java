package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.markdown.ComponentRenderer;
import dev.seeruk.bundle.text.TextComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Relays what happens in Discord to the game, e.g. messages in the bot's channel.
 */
public final class GameRelay {
    // As long as a message players can type in game. Longer ones are cut down, ending with
    // TOO_LONG, which counts towards the length.
    static final int MAX_MESSAGE_LENGTH = 256;
    static final String TOO_LONG = " [...]";

    // Replies are only shown in a hover, so they're kept short
    static final int MAX_REPLY_LENGTH = 100;

    // Messages (and replies) are also cut after this many lines, so one message can't fill chat
    static final int MAX_LINES = 5;

    private GameRelay() {}

    /**
     * Returns the message as it's shown in game chat, or nothing if there's nothing in it the game
     * can show (e.g. it's only an embed).
     *
     * @param message The message from Discord.
     * @param formats How messages from Discord are shown.
     */
    public static Optional<Component> message(DiscordMessage message, DiscordConfig.GameFormats formats) {
        final var content = shortened(
            ComponentRenderer.render(message.content(), message.mentions()),
            MAX_MESSAGE_LENGTH
        );

        // Attachments and stickers go after the message, separated by spaces
        final List<Component> parts = Stream.of(
            Stream.of(content).filter(text -> !text.getString().isEmpty()),
            message.attachments().stream().map(GameRelay::attachment),
            message.stickers().stream().map(GameRelay::sticker)
        ).flatMap(stream -> stream).toList();

        if (parts.isEmpty()) {
            return Optional.empty();
        }

        // A reply shows what it replied to when it's hovered over
        final var body = ComponentUtils.formatList(parts, Component.literal(" ")).copy();
        message.reply().ifPresent(reply -> body.withStyle(style -> style
            .withHoverEvent(new HoverEvent.ShowText(replied(reply)))));

        return Optional.of(formats.chat().format(Map.of(
            "display_name", ComponentRenderer.literal(message.displayName()),
            "username", Component.literal(message.username()),
            "message", body
        )));
    }

    /**
     * Returns the text cut down to at most the given number of characters (see
     * {@link TextComponents#length}), and {@link #MAX_LINES} lines, ending with {@link #TOO_LONG}
     * if it's been cut.
     */
    private static Component shortened(Component text, int maxLength) {
        final var plain = text.getString();
        var cut = Integer.MAX_VALUE;

        // Too long, so cut to leave room for TOO_LONG
        if (TextComponents.length(plain) > maxLength) {
            cut = maxLength - TOO_LONG.length();
        }

        // Too many lines, so cut at the end of the last line that's allowed, if that's sooner
        final var extraLine = lineBreak(plain, MAX_LINES);
        if (extraLine >= 0) {
            cut = Math.min(cut, TextComponents.length(plain.substring(0, extraLine)));
        }

        if (cut == Integer.MAX_VALUE) {
            return text;
        }

        return Component.empty().append(TextComponents.truncate(text, cut)).append(TOO_LONG);
    }

    /**
     * Returns where the given line break (counting from 1) is in the text, or -1 if there aren't
     * that many.
     */
    private static int lineBreak(String text, int count) {
        var position = -1;
        for (var i = 0; i < count; i++) {
            position = text.indexOf('\n', position + 1);
            if (position < 0) {
                return -1;
            }
        }
        return position;
    }

    /**
     * Returns what's shown when hovering over a reply, i.e. who it replied to, and what they said.
     */
    private static Component replied(DiscordMessage.Reply reply) {
        final var header = Component.literal("Replying to ")
            .append(ComponentRenderer.literal(reply.displayName()))
            .append(":")
            .withStyle(ChatFormatting.GRAY);
        final var content = shortened(ComponentRenderer.render(reply.content(), reply.mentions()), MAX_REPLY_LENGTH);

        // The header's grey is only its own, so it's not carried into the message
        return content.getString().isEmpty()
            ? header
            : Component.empty().append(header).append("\n").append(content);
    }

    /**
     * Returns an attachment as a link that opens it, e.g. {@code [image]}, or
     * {@code [file: notes.txt]}.
     */
    private static Component attachment(DiscordMessage.Attachment attachment) {
        final var label = attachment.image()
            ? Component.literal("[image]")
            : Component.literal("[file: ").append(ComponentRenderer.literal(attachment.fileName())).append("]");
        return ComponentRenderer.linked(label, attachment.url());
    }

    private static Component sticker(String name) {
        return Component.literal("[sticker: ").append(ComponentRenderer.literal(name)).append("]");
    }
}
