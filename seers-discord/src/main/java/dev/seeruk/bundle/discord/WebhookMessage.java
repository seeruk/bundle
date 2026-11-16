package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

/**
 * A message sent to Discord through the bot's webhook, so it can look like it's from anyone.
 *
 * @param username  The name it's shown as being from.
 * @param avatarUrl The avatar shown next to it.
 * @param content   The message, in Discord's Markdown.
 * @param embeds    Shown under the message, e.g. for linked items.
 */
public record WebhookMessage(String username, String avatarUrl, String content, List<MessageEmbed> embeds) {}
