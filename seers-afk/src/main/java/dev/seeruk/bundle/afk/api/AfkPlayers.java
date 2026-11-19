package dev.seeruk.bundle.afk.api;

import dev.seeruk.bundle.afk.AfkStatus;
import net.minecraft.server.level.ServerPlayer;

/**
 * AFK's public API for checking players' AFK status.
 */
public final class AfkPlayers {
    private AfkPlayers() {}

    /**
     * Returns whether the player is AFK.
     */
    public static boolean isAfk(ServerPlayer player) {
        return AfkStatus.isAfk(player);
    }
}
