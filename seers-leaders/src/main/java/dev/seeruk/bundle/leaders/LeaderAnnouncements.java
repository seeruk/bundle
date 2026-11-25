package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.names.PlayerNames;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chat messages about leaders, sent to everyone online.
 */
public final class LeaderAnnouncements {
    private LeaderAnnouncements() {}

    /**
     * Sends one message for every category with a new leader, a line each. Leaders taking over
     * from nobody aren't announced, as being first in an empty category isn't news, and neither
     * are categories that don't announce changes.
     */
    public static void announce(MinecraftServer server, LeadersConfig config, List<Leaders.Change> changes) {
        final var announcement = config.announcement();
        if (!announcement.enabled()) {
            return;
        }

        final var lines = changes.stream()
            .filter(change -> change.previous().isPresent() && change.leader().isPresent())
            .flatMap(change -> config.category(change.category())
                .filter(LeadersConfig.Category::announceChanges)
                .map(category -> announcement.format().format(Map.of(
                    "player", PlayerNames.displayName(server, change.leader().get()),
                    "category", category.title().format(Map.of())
                )))
                .stream())
            .toList();

        if (!lines.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(ComponentUtils.formatList(lines, Component.literal("\n")), false);
        }
    }

    /**
     * Sends one message for the whole period: a header, then a line for each winner.
     */
    public static void announcePeriod(
        MinecraftServer server,
        LeadersConfig config,
        String period,
        List<LeaderEvents.Winner> winners
    ) {
        final var announcement = config.periodAnnouncement();
        final var found = config.periods().stream().filter(p -> p.id().equals(period)).findFirst();
        if (!announcement.enabled() || winners.isEmpty() || found.isEmpty()) {
            return;
        }

        final var lines = new ArrayList<Component>();
        lines.add(announcement.header().format(Map.of("period", found.get().title().format(Map.of()))));
        for (final var winner : winners) {
            config.category(winner.category()).ifPresent(category -> lines.add(announcement.line().format(Map.of(
                "category", category.title().format(Map.of()),
                "player", PlayerNames.displayName(server, winner.player()),
                "value", Component.literal(category.unit().format(winner.value()))
            ))));
        }

        server.getPlayerList().broadcastSystemMessage(ComponentUtils.formatList(lines, Component.literal("\n")), false);
    }
}
