package dev.seeruk.bundle.chat;

import dev.seeruk.bundle.text.TextParsers;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Optional;

/**
 * Formats the messages players write, e.g. in chat or {@code /me}, with formatting tags and the
 * tags in {@link ChatTags}.
 */
public final class MessageFormatter {
    private static final NodeParser UNTRUSTED = TextParsers.untrusted(ChatTags.TAGS).build();
    private static final NodeParser TRUSTED = TextParsers.trusted(ChatTags.TAGS).serverPlaceholders().build();

    private MessageFormatter() {}

    /**
     * Formats a player's message. Trusted players (e.g. operators) can use every tag, and
     * placeholders. Messages that aren't formatted are returned as they are, so that clients don't
     * mark the messages as changed by the server if they don't need to be.
     *
     * @param player  The player who wrote the message, or null if it wasn't a player.
     * @param message The message, as the given player wrote it.
     */
    public static Component format(ServerPlayer player, Component message) {
        if (player == null) {
            return message;
        }

        final var parser = isTrusted(player) ? TRUSTED : UNTRUSTED;
        final var context = ServerPlaceholderContext.of(player).asParserContext()
            .with(ChatTags.KEY, tag -> ChatTags.resolve(player, tag));

        final var text = message.getString();
        final var formatted = parser.parseNode(text).toComponent(context);

        return isPlain(formatted, text) ? message : formatted;
    }

    static boolean isTrusted(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static boolean isPlain(Component formatted, String text) {
        final var styled = formatted.visit(
            (style, content) -> style.isEmpty() ? Optional.empty() : Optional.of(style),
            Style.EMPTY
        );

        return styled.isEmpty() && formatted.getString().equals(text);
    }
}
