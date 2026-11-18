package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

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
}
