package dev.seeruk.bundle.resourcezones.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Resource Zones' public events API.
 */
public final class ResourceZoneEvents {
    /**
     * Fired when chunks are added to or removed from a dimension's resource zones. Only fired when
     * they've actually changed.
     */
    public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(
        Changed.class,
        (listeners) -> (server, dimension) -> {
            for (Changed listener : listeners) {
                listener.onChanged(server, dimension);
            }
        }
    );

    /**
     * Fired when a player goes into a resource zone from outside one, however they got there (e.g.
     * walking, teleporting, or respawning). Moving between zone chunks isn't entering, and nor is
     * joining the server while in one, a zone being added around them, or spectating.
     */
    public static final Event<PlayerMoved> PLAYER_ENTERED = EventFactory.createArrayBacked(
        PlayerMoved.class,
        (listeners) -> player -> {
            for (PlayerMoved listener : listeners) {
                listener.onPlayerMoved(player);
            }
        }
    );

    /**
     * Fired when a player goes out of a resource zone, however they got there. Leaving the server
     * while in one isn't leaving it, and nor is the zone being removed around them, or spectating.
     */
    public static final Event<PlayerMoved> PLAYER_LEFT = EventFactory.createArrayBacked(
        PlayerMoved.class,
        (listeners) -> player -> {
            for (PlayerMoved listener : listeners) {
                listener.onPlayerMoved(player);
            }
        }
    );

    private ResourceZoneEvents() {}

    @FunctionalInterface
    public interface Changed {
        /**
         * @param server    The server the zones changed on.
         * @param dimension The dimension whose zone chunks changed.
         */
        void onChanged(MinecraftServer server, ResourceKey<Level> dimension);
    }

    @FunctionalInterface
    public interface PlayerMoved {
        /**
         * @param player The player who entered or left a resource zone.
         */
        void onPlayerMoved(ServerPlayer player);
    }
}
