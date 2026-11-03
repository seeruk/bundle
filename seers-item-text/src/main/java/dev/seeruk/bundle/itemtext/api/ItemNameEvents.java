package dev.seeruk.bundle.itemtext.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Public item name events API.
 */
public final class ItemNameEvents {
    /**
     * Fired when a player sets an item's name, but not when it's reset.
     */
    public static final Event<Written> WRITTEN = EventFactory.createArrayBacked(Written.class,
        (listeners) -> (player, item, name) -> {
            for (Written listener : listeners) {
                listener.onWritten(player, item, name);
            }
        }
    );

    private ItemNameEvents() {}

    @FunctionalInterface
    public interface Written {
        /**
         * @param player The player that set the name
         * @param item   The item that had its name set
         * @param name   The new name of the item
         */
        void onWritten(Player player, ItemStack item, Component name);
    }
}
