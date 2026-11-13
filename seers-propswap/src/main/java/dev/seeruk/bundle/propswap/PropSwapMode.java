package dev.seeruk.bundle.propswap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have PropSwap enabled. This is kept in-memory so it doesn't persist through
 * server restarts.
 *
 * <p>When players relog, this will need to be hooked into to disable PropSwap for the player.
 */
public final class PropSwapMode {
    /**
     * How long PropSwap can go unused before it turns itself off.
     */
    public static final int IDLE_TICKS = 5 * 60 * 20;

    // Player -> the server tick they last enabled or used PropSwap
    private static final Map<UUID, Integer> LAST_ACTIVE = new ConcurrentHashMap<>();

    private PropSwapMode() {}

    public static boolean isEnabled(Player player) {
        return LAST_ACTIVE.containsKey(player.getUUID());
    }

    /**
     * Toggle the current enabled state, and returns the new state.
     */
    public static boolean toggle(Player player) {
        if (isEnabled(player)) {
            disable(player);
            return false;
        }

        enable(player);
        return true;
    }

    public static void enable(Player player) {
        LAST_ACTIVE.put(player.getUUID(), now(player));
    }

    public static void disable(Player player) {
        LAST_ACTIVE.remove(player.getUUID());
    }

    public static void clear() {
        LAST_ACTIVE.clear();
    }

    /**
     * Records that the player just used PropSwap, so it doesn't turn itself off.
     */
    public static void touch(Player player) {
        LAST_ACTIVE.computeIfPresent(player.getUUID(), (id, tick) -> now(player));
    }

    /**
     * Turns PropSwap off for the player if it's unused for {@link #IDLE_TICKS}, and tells them.
     */
    public static void expireIfIdle(Player player, int now) {
        final var lastActive = LAST_ACTIVE.get(player.getUUID());
        if (lastActive == null || now - lastActive < IDLE_TICKS) {
            return;
        }

        disable(player);
        PropSwapReminder.hide(player);

        final var minutes = IDLE_TICKS / (60 * 20);
        player.sendSystemMessage(Component.literal("PropSwap disabled after " + minutes + " minutes without use"));
    }

    public static void expireIdle(MinecraftServer server) {
        if (LAST_ACTIVE.isEmpty()) {
            return;
        }

        for (final var player : server.getPlayerList().getPlayers()) {
            expireIfIdle(player, server.getTickCount());
        }
    }

    private static int now(Player player) {
        return player.level().getServer().getTickCount();
    }
}
