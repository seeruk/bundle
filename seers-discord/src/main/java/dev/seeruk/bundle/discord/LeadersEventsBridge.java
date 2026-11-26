package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.names.PlayerNames;
import dev.seeruk.bundle.discord.config.EventsConfig;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sends new leaders, and periods' winners, from Seer's Leaders to Discord, when they're worth
 * announcing. Seer's Leaders is optional, so this is its own class, which is only loaded (and so
 * only needs the Leaders mod) if that mod is.
 */
final class LeadersEventsBridge {
    private LeadersEventsBridge() {}

    static void register(EventsConfig events) {
        // New leaders found together (in one refresh, so in one tick) are sent together. Only used
        // on the server thread.
        final List<MessageEmbed> changed = new ArrayList<>();

        LeaderEvents.CHANGED.register((server, change) -> {
            if (!change.announceable()) {
                return;
            }

            final var leader = change.leader().orElseThrow();
            DiscordRelay.leadersChanged(
                leader,
                displayName(server, leader),
                username(server, leader),
                displayName(server, change.previous().orElseThrow()),
                change.title().getString(),
                events
            ).ifPresent(changed::add);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!changed.isEmpty()) {
                WebhookMessage.packed(changed).forEach(DiscordBot::send);
                changed.clear();
            }
        });

        LeaderEvents.PERIOD_ENDED.register((server, end) -> {
            if (!end.announceable()) {
                return;
            }

            final var winners = end.winners().stream()
                .map(winner -> new DiscordRelay.LeadersWinner(
                    winner.title().getString(),
                    displayName(server, winner.player()),
                    username(server, winner.player()),
                    winner.displayValue()
                ))
                .toList();
            DiscordRelay.leadersPeriodEnded(end.title().getString(), winners, events).forEach(DiscordBot::send);
        });
    }

    private static String displayName(MinecraftServer server, UUID id) {
        return PlayerNames.displayName(server, id).getString();
    }

    private static String username(MinecraftServer server, UUID id) {
        return PlayerNames.username(server, id).orElse("");
    }
}
