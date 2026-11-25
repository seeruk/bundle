package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.names.PlayerNames;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tells everyone online when a category has a new leader.
 */
public final class LeaderAnnouncements {
    private LeaderAnnouncements() {}

    public static void announce(MinecraftServer server, LeadersConfig config, String category, Optional<UUID> leader) {
        final var announcement = config.announcement();
        if (!announcement.enabled() || leader.isEmpty()) {
            return;
        }

        config.category(category).ifPresent(found -> server.getPlayerList().broadcastSystemMessage(
            announcement.format().format(Map.of(
                "player", PlayerNames.displayName(server, leader.get()),
                "category", found.title().format(Map.of())
            )),
            false
        ));
    }
}
