package dev.seeruk.bundle.itemtext.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Public item lore events API.
 */
public final class ItemLoreEvents {
    /**
     * Fired when a player edits an item's lore. Only fired when an edit is successful.
     */
    public static final Event<Edited> EDITED = EventFactory.createArrayBacked(Edited.class,
        (listeners) -> (player, item, line, lore) -> {
            for (Edited listener : listeners) {
                listener.onEdited(player, item, line, lore);
            }
        }
    );

    private ItemLoreEvents() {}

    @FunctionalInterface
    public interface Edited {
        /**
         * @param player   The player who set the lore
         * @param item     The item that had its lore modified
         * @param original The original lore of the item, before editing
         * @param edited   The new lore of the item, after editing
         */
        void onEdited(Player player, ItemStack item, List<Component> original, List<Component> edited);
    }
}
