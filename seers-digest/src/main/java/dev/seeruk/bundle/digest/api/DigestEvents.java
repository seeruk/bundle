package dev.seeruk.bundle.digest.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

/**
 * Public digest events API.
 */
public final class DigestEvents {
    /**
     * Fired when an issue is published, if anyone was active since the last one.
     */
    public static final Event<Published> PUBLISHED = EventFactory.createArrayBacked(
        Published.class,
        (listeners) -> (server, issue) -> {
            for (Published listener : listeners) {
                listener.onPublished(server, issue);
            }
        }
    );

    private DigestEvents() {}

    @FunctionalInterface
    public interface Published {
        /**
         * @param server The server it was published on
         * @param issue  The issue
         */
        void onPublished(MinecraftServer server, Issue issue);
    }
}
