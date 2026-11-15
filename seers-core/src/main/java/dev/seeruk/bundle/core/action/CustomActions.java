package dev.seeruk.bundle.core.action;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handlers for Vanilla's custom click actions, which clients send when a player clicks a dialog
 * button (or text) with a {@code minecraft:custom} action. Vanilla only logs them, so each mod
 * registers handlers for its own IDs here.
 *
 * <p>Any client can send any custom action at any time. Handlers must treat the payload as
 * untrusted, and check everything like you would with a command.
 */
public final class CustomActions {
    private static final Map<Identifier, Handler> HANDLERS = new ConcurrentHashMap<>();

    private CustomActions() {}

    @FunctionalInterface
    public interface Handler {
        /**
         * @param player  The player who sent the action.
         * @param payload The action's payload, e.g. a dialog's input values. Can be empty.
         */
        void handle(ServerPlayer player, CompoundTag payload);
    }

    public static void register(Identifier id, Handler handler) {
        HANDLERS.put(id, handler);
    }

    /**
     * Passes the action to its handler, if one is registered for its ID.
     *
     * @param player  The player who sent the action.
     * @param id      The action's ID.
     * @param payload The action's payload, which may not be a compound...
     */
    public static void handle(ServerPlayer player, Identifier id, Object payload) {
        final var handler = HANDLERS.get(id);
        if (handler != null) {
            handler.handle(player, payload instanceof CompoundTag tag ? tag : new CompoundTag());
        }
    }
}
