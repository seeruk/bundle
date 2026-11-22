package dev.seeruk.bundle.resourcezones.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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

    private ResourceZoneEvents() {}

    @FunctionalInterface
    public interface Changed {
        /**
         * @param server    The server the zones changed on.
         * @param dimension The dimension whose zone chunks changed.
         */
        void onChanged(MinecraftServer server, ResourceKey<Level> dimension);
    }
}
