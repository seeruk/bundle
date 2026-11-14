package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkEvents;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are AFK, and marks them as AFK, or back, when they should be. Activity is
 * Vanilla's {@link ServerPlayer#getLastActionTime()}, which also drives the idle kick.
 *
 * <p>AFK status must be stored here in-memory, because the {@code /afk} command means a players
 * AFK status is not determined by their last action time alone.
 */
public final class AfkStatus {
    // AFK player -> their last action time when they went AFK, so any action since brings them
    // back. Concurrent, as statuses are read from many threads.
    private static final Map<UUID, Long> AFK = new ConcurrentHashMap<>();

    private AfkStatus() {}

    public static boolean isAfk(Player player) {
        return AFK.containsKey(player.getUUID());
    }

    /**
     * Toggles the player's AFK status, and returns the new status.
     */
    public static boolean toggle(ServerPlayer player, AfkReason reason) {
        final var afk = !isAfk(player);
        set(player, afk, reason);
        return afk;
    }

    /**
     * Marks the player as AFK if they haven't done anything for the timeout, or back if they've
     * done something since they went AFK.
     *
     * @param player        The player to update.
     * @param now           The current time, in {@link Util#getMillis()} time.
     * @param timeoutMillis The AFK timeout, in milliseconds.
     */
    public static void update(ServerPlayer player, long now, long timeoutMillis) {
        final var lastAction = AFK.get(player.getUUID());
        if (lastAction != null) {
            if (player.getLastActionTime() != lastAction) {
                set(player, false, AfkReason.ACTIVITY);
            }
        } else if (now - player.getLastActionTime() >= timeoutMillis) {
            set(player, true, AfkReason.TIMEOUT);
        }
    }

    public static void updateAll(MinecraftServer server, long timeoutMillis) {
        final var now = Util.getMillis();
        for (final var player : server.getPlayerList().getPlayers()) {
            update(player, now, timeoutMillis);
        }
    }

    /**
     * Forgets the player without announcing it, e.g. because they're disconnecting.
     */
    public static void forget(Player player) {
        AFK.remove(player.getUUID());
    }

    public static void clear() {
        AFK.clear();
    }

    private static void set(ServerPlayer player, boolean afk, AfkReason reason) {
        final var changed = afk
            ? AFK.put(player.getUUID(), player.getLastActionTime()) == null
            : AFK.remove(player.getUUID()) != null;

        if (changed) {
            AfkEvents.STATUS_CHANGED.invoker().onStatusChanged(player, afk, reason);
        }
    }
}
