package dev.seeruk.bundle.disposal.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Disposal's public events API.
 */
public final class DisposalEvents {
    /**
     * Fired when a player closes the disposal inventory with items in it, just after the items have
     * been destroyed. The list of items is a copy of the destroyed stacks and will never be empty.
     */
    public static final Event<Disposed> DISPOSED = EventFactory.createArrayBacked(
        Disposed.class,
        (listeners) -> (player, items) -> {
            for (Disposed listener : listeners) {
                listener.onDisposed(player, items);
            }
        }
    );

    private DisposalEvents() {}

    @FunctionalInterface
    public interface Disposed {
        /**
         * @param player The player who disposed of the items
         * @param items  Copies of the disposed item stacks
         */
        void onDisposed(Player player, List<ItemStack> items);
    }
}
