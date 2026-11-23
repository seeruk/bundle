package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are in resource zones, and fires {@link ResourceZoneEvents#PLAYER_ENTERED}
 * and {@link ResourceZoneEvents#PLAYER_LEFT} when that changes.
 *
 * <p>Players are tracked by UUID, so respawning (which makes a new player) only counts as entering
 * a zone if they weren't already in one.
 */
public final class ZonePresence {
    // Player -> whether they're in a zone. Concurrent, as it can be read from many threads.
    private static final Map<UUID, Boolean> IN_ZONE = new ConcurrentHashMap<>();

    private ZonePresence() {}

    public static boolean isInZone(ServerPlayer player) {
        return IN_ZONE.getOrDefault(player.getUUID(), false);
    }

    public static void updateAll(MinecraftServer server) {
        for (final var player : server.getPlayerList().getPlayers()) {
            update(player);
        }
    }

    /**
     * Notes whether the player is in a zone, firing an event if they've entered or left one since
     * they were last seen. The first time they're seen (e.g. just after joining), nothing's fired.
     */
    public static void update(ServerPlayer player) {
        final var level = player.level();
        final var inZone = ResourceZones.contains(level.getServer(), level.dimension(), player.chunkPosition());

        final var wasInZone = IN_ZONE.put(player.getUUID(), inZone);
        if (wasInZone == null || wasInZone == inZone) {
            return;
        }

        final var event = inZone ? ResourceZoneEvents.PLAYER_ENTERED : ResourceZoneEvents.PLAYER_LEFT;
        event.invoker().onPlayerMoved(player);
    }

    /**
     * Forgets the player without firing anything, e.g. because they're disconnecting.
     */
    public static void forget(ServerPlayer player) {
        IN_ZONE.remove(player.getUUID());
    }

    public static void clear() {
        IN_ZONE.clear();
    }
}
