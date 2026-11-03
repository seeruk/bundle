package dev.seeruk.bundle.itemtext.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Public item lore events API.
 */
public final class ItemLoreEvents {
    /**
     * Fired when a player adds lore to an item (add, set, insert), but not when it's removed or
     * cleared.
     */
    public static final Event<Written> WRITTEN = EventFactory.createArrayBacked(Written.class,
        (listeners) -> (player, item, line, lore) -> {
            for (Written listener : listeners) {
                listener.onWritten(player, item, line, lore);
            }
        }
    );

    private ItemLoreEvents() {}

    @FunctionalInterface
    public interface Written {
        /**
         * @param player The player who set the lore
         * @param item   The item that had its lore written to
         * @param line   The line number that was written to (0 indexed)
         * @param lore   The lore that was written
         */
        void onWritten(Player player, ItemStack item, int line, Component lore);
    }
}
