package dev.seeruk.bundle.afk.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * AFK's public events API.
 */
public final class AfkEvents {
    /**
     * Fired when a player goes AFK, or comes back, by any means. Not fired when an AFK player
     * disconnects.
     */
    public static final Event<StatusChanged> STATUS_CHANGED = EventFactory.createArrayBacked(
        StatusChanged.class,
        (listeners) -> (player, isAfk, reason) -> {
            for (StatusChanged listener : listeners) {
                listener.onStatusChanged(player, isAfk, reason);
            }
        }
    );

    private AfkEvents() {}

    @FunctionalInterface
    public interface StatusChanged {
        /**
         * @param player The player whose AFK status changed.
         * @param isAfk  The AFK status the player has changed to.
         * @param reason Why the player's AFK status changed.
         */
        void onStatusChanged(ServerPlayer player, boolean isAfk, AfkReason reason);
    }
}
