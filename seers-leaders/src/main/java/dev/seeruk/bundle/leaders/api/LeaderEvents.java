package dev.seeruk.bundle.leaders.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;
import java.util.UUID;

/**
 * Public leaders events API.
 */
public final class LeaderEvents {
    /**
     * Fired when a category's leader (who holds first place) changes. Not fired the first time a
     * category's leader is worked out, as there's nobody to compare with.
     */
    public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(
        Changed.class,
        (listeners) -> (server, category, previous, leader) -> {
            for (Changed listener : listeners) {
                listener.onLeaderChanged(server, category, previous, leader);
            }
        }
    );

    private LeaderEvents() {}

    @FunctionalInterface
    public interface Changed {
        /**
         * @param server   The server the leader changed on
         * @param category The category's ID
         * @param previous Who led the category before, if anyone
         * @param leader   Who leads the category now, if anyone
         */
        void onLeaderChanged(MinecraftServer server, String category, Optional<UUID> previous, Optional<UUID> leader);
    }
}
