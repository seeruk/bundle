package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.config.DiscordConfig;
import dev.seeruk.bundle.discord.markdown.ComponentRenderer;
import dev.seeruk.bundle.text.TextColours;
import dev.seeruk.bundle.text.TextComponents;
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
    // How much of a message is shown in chat, i.e. as long as a message players can type in game,
    // and few enough lines that one message can't fill chat. Longer ones are cut down, ending
    // with TOO_LONG, which counts towards the length.
    static final Limit MESSAGE = new Limit(256, 5);
    static final String TOO_LONG = " [...]";

    // How much of a cut down message is shown when hovering over its TOO_LONG, i.e. as much as
    // fits on screen in a hover
    static final Limit FULL_MESSAGE = new Limit(1000, 20);

    // How much of a replied-to message is shown, as it's only extra context
    static final Limit REPLY = new Limit(100, 5);

    private GameRelay() {}

    /**
     * How much text is shown.
     *
     * @param length The most characters shown (see {@link TextComponents#length}).
     * @param lines  The most lines shown.
     */
    record Limit(int length, int lines) {}

    /**
     * Returns the message as it's shown in game chat, or nothing if there's nothing in it the game
     * can show (e.g. it's only an embed).
     *
     * @param message The message from Discord.
     * @param formats How messages from Discord are shown.
     */
    public static Optional<Component> message(DiscordMessage message, DiscordConfig.GameSettings formats) {
        final var rendered = ComponentRenderer.render(message.content(), message.mentions());

        // If it's cut down, hovering over the end shows the rest of it
        final var content = shortened(
            rendered,
            MESSAGE,
            Component.literal(TOO_LONG).withStyle(style -> style
                .withColor(TextColours.HOVERABLE)
                .withHoverEvent(new HoverEvent.ShowText(shortened(rendered, FULL_MESSAGE))))
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

        return Optional.of(formats.chat().format(Map.of(
            "reply", message.reply().map(reply -> replied(reply, formats)).orElse(Component.empty()),
            "display_name", ComponentRenderer.literal(message.displayName()),
            "username", Component.literal(message.username()),
            "message", ComponentUtils.formatList(parts, Component.literal(" "))
        )));
    }

    /**
     * Returns the text cut down to the limit, ending with {@link #TOO_LONG} if it's been cut.
     */
    private static Component shortened(Component text, Limit limit) {
        return shortened(text, limit, Component.literal(TOO_LONG));
    }

    /**
     * Returns the text cut down to the limit, ending with the given marker (which should show
     * {@link #TOO_LONG}) if it's been cut.
     */
    private static Component shortened(Component text, Limit limit, Component tooLong) {
        final var plain = text.getString();
        var cut = Integer.MAX_VALUE;

        // Too long, so cut to leave room for TOO_LONG
        if (TextComponents.length(plain) > limit.length()) {
            cut = limit.length() - TOO_LONG.length();
        }

        // Too many lines, so cut at the end of the last line that's allowed, if that's sooner
        final var extraLine = lineBreak(plain, limit.lines());
        if (extraLine >= 0) {
            cut = Math.min(cut, TextComponents.length(plain.substring(0, extraLine)));
        }

        if (cut == Integer.MAX_VALUE) {
            return text;
        }

        return Component.empty().append(TextComponents.truncate(text, cut)).append(tooLong);
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
     * Returns the reply format, filled in with the message that's been replied to.
     */
    private static Component replied(DiscordMessage.Reply reply, DiscordConfig.GameSettings formats) {
        return formats.reply().format(Map.of(
            "display_name", ComponentRenderer.literal(reply.displayName()),
            "username", Component.literal(reply.username()),
            "message", shortened(ComponentRenderer.render(reply.content(), reply.mentions()), REPLY)
        ));
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
