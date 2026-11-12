package dev.seeruk.bundle.propswap.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PropSwap's public events API.
 */
public final class PropSwapEvents {
    /**
     * Fired when a player PropSwaps a block. Only fired when the block has actually changed
     * (though, not sure how it could not change currently...).
     */
    public static final Event<Swapped> SWAPPED = EventFactory.createArrayBacked(Swapped.class,
        (listeners) -> (player, level, pos, original, swapped) -> {
            for (Swapped listener : listeners) {
                listener.onSwapped(player, level, pos, original, swapped);
            }
        }
    );

    private PropSwapEvents() {}

    @FunctionalInterface
    public interface Swapped {
        /**
         * @param player   The player that PropSwapped the block
         * @param level    The level the block is in
         * @param pos      The position of the block
         * @param original The block's state before the swap
         * @param swapped  The block's state after the swap
         */
        void onSwapped(Player player, Level level, BlockPos pos, BlockState original, BlockState swapped);
    }
}
