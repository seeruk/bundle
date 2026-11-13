package dev.seeruk.bundle.propswap;

import dev.seeruk.bundle.propswap.swap.Swaps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Shared logic for server and client used to determine whether PropSwap should act on a block.
 */
public final class PropSwapTargets {
    private PropSwapTargets() {}

    /**
     * Whether the given item is a tool that can change the block, and a swap supports it.
     */
    public static boolean supports(ItemStack stack, BlockState state) {
        return PropSwapTool.canChange(stack, state) && Swaps.supports(state);
    }

    /**
     * {@link #supports}, plus the checks Vanilla makes before a player can change a block.
     */
    public static boolean allowed(Player player, ItemStack stack, BlockState state, BlockHitResult hit) {
        return supports(stack, state)
            && player.mayBuild()
            && player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), stack);
    }
}
