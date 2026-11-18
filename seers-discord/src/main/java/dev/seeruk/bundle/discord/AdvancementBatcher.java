package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gathers advancements and allows them to be sent together all at once in a single Discord message
 * (so they don't overwhelm the JDA queue).
 *
 * <p>It's only used on the server thread, where advancements are completed, and ticks end.
 */
final class AdvancementBatcher {
    // Each player's completed advancements' embeds, in the order they were completed
    private final Map<ServerPlayer, List<MessageEmbed>> completed = new LinkedHashMap<>();

    void add(ServerPlayer player, MessageEmbed embed) {
        completed.computeIfAbsent(player, ignored -> new ArrayList<>()).add(embed);
    }

    /**
     * Returns a message for each player who's completed advancements since this was last called,
     * which are then forgotten. Each is either their advancements' embeds, or a summary, if there
     * are too many to fit in one message.
     */
    List<WebhookMessage> flush(EventsConfig events) {
        final var messages = new ArrayList<WebhookMessage>();
        completed.forEach((player, embeds) -> {
            if (fitInOneMessage(embeds)) {
                messages.add(WebhookMessage.embeds(embeds));
            } else {
                DiscordRelay.advancementSummary(player, embeds.size(), events)
                    .map(WebhookMessage::embed)
                    .ifPresent(messages::add);
            }
        });

        completed.clear();
        return messages;
    }

    /**
     * Whether the embeds fit in one Discord message, which can only have so many, with only so much
     * text between them.
     */
    private static boolean fitInOneMessage(List<MessageEmbed> embeds) {
        return embeds.size() <= Message.MAX_EMBED_COUNT
            && embeds.stream().mapToInt(MessageEmbed::getLength).sum() <= MessageEmbed.EMBED_MAX_LENGTH_BOT;
    }
}
