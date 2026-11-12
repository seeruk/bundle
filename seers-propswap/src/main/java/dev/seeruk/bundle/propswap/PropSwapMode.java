package dev.seeruk.bundle.propswap;

import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players have PropSwap enabled. This is kept in-memory so it doesn't persist through
 * server restarts.
 *
 * <p>When players relog, this will need to be hooked into to disable PropSwap for the player.
 */
public final class PropSwapMode {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private PropSwapMode() {}

    public static boolean isEnabled(Player player) {
        return ENABLED.contains(player.getUUID());
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
        ENABLED.add(player.getUUID());
    }

    public static void disable(Player player) {
        ENABLED.remove(player.getUUID());
    }

    public static void clear() {
        ENABLED.clear();
    }
}
