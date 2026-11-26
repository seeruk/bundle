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
     * Sends one message for every change worth announcing (see
     * {@link LeaderEvents.Change#announceable()}), a line each.
     */
    public static void announce(MinecraftServer server, LeadersConfig config, List<LeaderEvents.Change> changes) {
        final var announcement = config.announcement();
        if (!announcement.enabled()) {
            return;
        }

        final var lines = changes.stream()
            .filter(LeaderEvents.Change::announceable)
            .map(change -> announcement.format().format(Map.of(
                "player", PlayerNames.displayName(server, change.leader().orElseThrow()),
                "category", change.title()
            )))
            .toList();

        if (!lines.isEmpty()) {
            broadcast(server, lines);
        }
    }

    /**
     * Sends one message for the whole period, if it's worth announcing (see
     * {@link LeaderEvents.PeriodEnd#announceable()}): a header, then a line for each winner.
     */
    public static void announcePeriod(MinecraftServer server, LeadersConfig config, LeaderEvents.PeriodEnd end) {
        final var announcement = config.periodAnnouncement();
        if (!announcement.enabled() || !end.announceable()) {
            return;
        }

        final var lines = new ArrayList<Component>();
        lines.add(announcement.header().format(Map.of("period", end.title())));
        end.winners().forEach(winner -> lines.add(announcement.line().format(Map.of(
            "category", winner.title(),
            "player", PlayerNames.displayName(server, winner.player()),
            "value", Component.literal(winner.displayValue())
        ))));

        broadcast(server, lines);
    }

    // Sends the lines to everyone online as one message, so they arrive together
    private static void broadcast(MinecraftServer server, List<Component> lines) {
        server.getPlayerList().broadcastSystemMessage(ComponentUtils.formatList(lines, Component.literal("\n")), false);
    }
}
