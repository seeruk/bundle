package dev.seeruk.bundle.core.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

/**
 * Events for things that happen to players, which Vanilla and Fabric don't have events for.
 */
public final class PlayerEvents {
    /**
     * Fired when a player completes an advancement and it's announced in chat, which Vanilla only
     * does for advancements that are meant to be, with the showAdvancementMessages game rule on
     * (see {@link PlayerAdvancements#award}).
     */
    public static final Event<AdvancementMessage> ADVANCEMENT_MESSAGE = EventFactory.createArrayBacked(
        AdvancementMessage.class,
        (listeners) -> (player, advancement, message) -> {
            for (AdvancementMessage listener : listeners) {
                listener.onAdvancementMessage(player, advancement, message);
            }
        }
    );

    /**
     * Fired when a player dies and their death message is shown, which Vanilla only does with the
     * showDeathMessages game rule on.
     */
    public static final Event<DeathMessage> DEATH_MESSAGE = EventFactory.createArrayBacked(
        DeathMessage.class,
        (listeners) -> (player, message) -> {
            for (DeathMessage listener : listeners) {
                listener.onDeathMessage(player, message);
            }
        }
    );

    private PlayerEvents() {}

    @FunctionalInterface
    public interface AdvancementMessage {
        /**
         * @param player      The player who completed the advancement.
         * @param advancement The advancement they completed, which always has a display.
         * @param message     Its announcement, e.g. "Seer has made the advancement [Stone Age]".
         */
        void onAdvancementMessage(ServerPlayer player, AdvancementHolder advancement, Component message);
    }

    @FunctionalInterface
    public interface DeathMessage {
        /**
         * @param player  The player who died.
         * @param message Their death message, e.g. "Seer was slain by Zombie".
         */
        void onDeathMessage(ServerPlayer player, Component message);
    }
}
