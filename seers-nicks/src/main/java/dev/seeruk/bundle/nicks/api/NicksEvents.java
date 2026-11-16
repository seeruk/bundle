package dev.seeruk.bundle.nicks.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

import java.util.Optional;

/**
 * Public nicknames events API.
 */
public final class NicksEvents {
    /**
     * Fired when a player's nickname changes. Only fired when an edit is successful, and the
     * nickname has actually changed. The player may be offline, if an admin changed it.
     */
    public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(
        Changed.class,
        (listeners) -> (server, player, original, edited) -> {
            for (Changed listener : listeners) {
                listener.onNickChanged(server, player, original, edited);
            }
        }
    );

    private NicksEvents() {}

    @FunctionalInterface
    public interface Changed {
        /**
         * @param server   The server the nickname changed on
         * @param player   The player whose nickname changed
         * @param original The player's original nickname, if they had one
         * @param edited   The player's new nickname, if they have one
         */
        void onNickChanged(MinecraftServer server, NameAndId player, Optional<Component> original, Optional<Component> edited);
    }
}
