package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkEvents;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players are AFK, and marks them as AFK, or back, when they should be. Activity is
 * Vanilla's {@link ServerPlayer#getLastActionTime()}, which also drives the idle kick.
 *
 * <p>AFK status must be stored here in-memory, because the {@code /afk} command means a player's
 * AFK status is not determined by their last action time alone.
 *
 * <p>Idle time is counted in server ticks, rather than real time, so time spent with the server
 * paused (e.g. a singleplayer world in the pause menu) doesn't count.
 */
public final class AfkStatus {
    // Player -> their activity. Concurrent, as statuses are read from many threads.
    private static final Map<UUID, Activity> ACTIVITY = new ConcurrentHashMap<>();

    /**
     * @param lastAction The player's last action time, as last seen.
     * @param since      The server tick their idle time is counted from.
     * @param afk        Whether they're AFK.
     */
    private record Activity(long lastAction, int since, boolean afk) {}

    private AfkStatus() {}

    public static boolean isAfk(Player player) {
        final var activity = ACTIVITY.get(player.getUUID());
        return activity != null && activity.afk();
    }

    /**
     * Toggles the player's AFK status. Their idle time is counted from now, as toggling it shows
     * they're there.
     *
     * @param player The player whose status to toggle.
     * @param tick   The current server tick.
     * @param reason Why their status is changing.
     */
    public static void toggle(ServerPlayer player, int tick, AfkReason reason) {
        set(player, new Activity(player.getLastActionTime(), tick, !isAfk(player)), reason);
    }

    /**
     * Marks the player as AFK if they haven't done anything for the timeout, or back if they've
     * done something since they were last seen.
     *
     * @param player       The player to update.
     * @param tick         The current server tick.
     * @param timeoutTicks The AFK timeout, in ticks.
     */
    public static void update(ServerPlayer player, int tick, int timeoutTicks) {
        final var lastAction = player.getLastActionTime();
        final var activity = ACTIVITY.get(player.getUUID());

        if (activity == null || activity.lastAction() != lastAction) {
            set(player, new Activity(lastAction, tick, false), AfkReason.ACTIVITY);
        } else if (!activity.afk() && tick - activity.since() >= timeoutTicks) {
            set(player, new Activity(lastAction, activity.since(), true), AfkReason.TIMEOUT);
        }
    }

    public static void updateAll(MinecraftServer server, int timeoutTicks) {
        for (final var player : server.getPlayerList().getPlayers()) {
            update(player, server.getTickCount(), timeoutTicks);
        }
    }

    /**
     * Keeps the player's status and idle time as they were before they respawned, as respawning
     * resets their last action time, even when it's automatic (e.g. with the
     * {@code doImmediateRespawn} game rule).
     */
    public static void respawned(ServerPlayer player) {
        ACTIVITY.computeIfPresent(
            player.getUUID(), (id, activity) ->
                new Activity(player.getLastActionTime(), activity.since(), activity.afk())
        );
    }

    /**
     * Forgets the player without announcing it, e.g. because they're disconnecting.
     */
    public static void forget(Player player) {
        ACTIVITY.remove(player.getUUID());
    }

    public static void clear() {
        ACTIVITY.clear();
    }

    /**
     * Stores the player's activity, and fires {@link AfkEvents#STATUS_CHANGED} if that changes
     * whether they're AFK.
     */
    private static void set(ServerPlayer player, Activity activity, AfkReason reason) {
        final var previous = ACTIVITY.put(player.getUUID(), activity);
        final var wasAfk = previous != null && previous.afk();

        if (wasAfk != activity.afk()) {
            AfkEvents.STATUS_CHANGED.invoker().onStatusChanged(player, activity.afk(), reason);
        }
    }
}
