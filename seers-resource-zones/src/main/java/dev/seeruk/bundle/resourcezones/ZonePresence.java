package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are in resource zones, and fires {@link ResourceZoneEvents#PLAYER_ENTERED}
 * and {@link ResourceZoneEvents#PLAYER_LEFT} when they move into or out of one.
 *
 * <p>Players are tracked by UUID, so respawning (which makes a new player) only counts as entering
 * a zone if they weren't already in one. Spectators aren't tracked, as they aren't really there,
 * and nor are players editing zones, so editing doesn't count as entering or leaving them.
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
     * they were last seen. The first time they're seen (e.g. just after joining, or after being a
     * spectator or editing), nothing's fired.
     */
    public static void update(ServerPlayer player) {
        if (player.isSpectator() || ZoneEditor.isEditing(player)) {
            forget(player);
            return;
        }

        final var inZone = check(player);
        final var wasInZone = IN_ZONE.put(player.getUUID(), inZone);
        if (wasInZone == null || wasInZone == inZone) {
            return;
        }

        final var event = inZone ? ResourceZoneEvents.PLAYER_ENTERED : ResourceZoneEvents.PLAYER_LEFT;
        event.invoker().onPlayerMoved(player);
    }

    /**
     * Notes whether each player we know of in the dimension is in a zone, without firing anything,
     * as the zones changed around them rather than them moving.
     */
    public static void recheckQuietly(MinecraftServer server, ResourceKey<Level> dimension) {
        for (final var player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() == dimension) {
                IN_ZONE.computeIfPresent(player.getUUID(), (uuid, wasInZone) -> check(player));
            }
        }
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

    private static boolean check(ServerPlayer player) {
        final var level = player.level();
        return ResourceZones.contains(level.getServer(), level.dimension(), player.chunkPosition());
    }
}
