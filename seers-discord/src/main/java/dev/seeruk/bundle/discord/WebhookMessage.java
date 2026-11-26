package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;

/**
 * A message sent to Discord through the bot's webhook, so it can look like it's from anyone.
 *
 * @param username  The name it's shown as being from, or null for the webhook's own.
 * @param avatarUrl The avatar shown next to it, or null for the webhook's own.
 * @param content   The message, in Discord's Markdown, which can be empty if there are embeds.
 * @param embeds    Shown under the message, e.g. for linked items.
 */
public record WebhookMessage(String username, String avatarUrl, String content, List<MessageEmbed> embeds) {
    /**
     * Returns a message that's only an embed, sent as the webhook itself, e.g. for events.
     */
    public static WebhookMessage embed(MessageEmbed embed) {
        return embeds(List.of(embed));
    }

    /**
     * Returns a message that's only embeds, sent as the webhook itself. There can be at most
     * {@link Message#MAX_EMBED_COUNT}.
     */
    public static WebhookMessage embeds(List<MessageEmbed> embeds) {
        return new WebhookMessage(null, null, "", List.copyOf(embeds));
    }

    /**
     * Returns the embeds in as few messages as they fit in, in order, each sent as the webhook
     * itself. A message can have at most {@link Message#MAX_EMBED_COUNT} embeds, with at most
     * {@link MessageEmbed#EMBED_MAX_LENGTH_BOT} characters between them.
     */
    public static List<WebhookMessage> packed(List<MessageEmbed> embeds) {
        final var messages = new ArrayList<WebhookMessage>();
        var current = new ArrayList<MessageEmbed>();
        var length = 0; // Length of the current message's text.

        for (final var embed : embeds) {
            // Check if we've hit the max embed count / message length with the current list of
            // embeds we're aiming to send.
            final var full = current.size() == Message.MAX_EMBED_COUNT
                || length + embed.getLength() > MessageEmbed.EMBED_MAX_LENGTH_BOT;

            // If we've hit the max embed count / message length (before adding the current embed),
            // add the current list of embeds to the messages we're going to send.
            if (full && !current.isEmpty()) {
                messages.add(embeds(current));
                current = new ArrayList<>();
                length = 0;
            }

            current.add(embed);
            length += embed.getLength();
        }

        if (!current.isEmpty()) {
            messages.add(embeds(current));
        }
        return messages;
    }
}
