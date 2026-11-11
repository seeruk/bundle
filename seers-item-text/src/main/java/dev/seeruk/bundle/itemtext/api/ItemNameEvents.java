package dev.seeruk.bundle.itemtext.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Public item name events API.
 */
public final class ItemNameEvents {
    /**
     * Fired when a player modifies an item's name.
     */
    public static final Event<Edited> EDITED = EventFactory.createArrayBacked(Edited.class,
        (listeners) -> (player, item, original, edited) -> {
            for (Edited listener : listeners) {
                listener.onEdited(player, item, original, edited);
            }
        }
    );

    private ItemNameEvents() {}

    @FunctionalInterface
    public interface Edited {
        /**
         * @param player   The player that modified the name
         * @param item     The item that had its name modified
         * @param original The original custom name of the item, if present
         * @param edited   The new custom name of the item, if present
         */
        void onEdited(Player player, ItemStack item, Optional<Component> original, Optional<Component> edited);
    }
}
