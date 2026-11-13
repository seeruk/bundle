package dev.seeruk.bundle.propswap;

import dev.seeruk.bundle.propswap.api.PropSwapEvents;
import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import dev.seeruk.bundle.propswap.swap.SwapInput;
import dev.seeruk.bundle.propswap.swap.Swaps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Handles players using a tool on a block while PropSwap is enabled.
 */
public final class PropSwapInteraction {
    // Same flags as the debug stick, so neighbours don't update their shape.
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private PropSwapInteraction() {}

    /**
     * Listens to {@link net.fabricmc.fabric.api.event.player.UseBlockCallback}.
     * Returns a {@code PASS} whenever PropSwap shouldn't do anything, leaving it for Vanilla.
     */
    public static InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || player.isSpectator() || !PropSwapMode.isEnabled(player)) {
            return InteractionResult.PASS;
        }

        final var stack = player.getItemInHand(hand);
        final var pos = hit.getBlockPos();
        final var original = level.getBlockState(pos);

        if (!PropSwapTool.canChange(stack, original) || !player.mayBuild() || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResult.PASS;
        }

        final var input = new SwapInput(original, hit.getDirection(), player.getDirection(), player.isShiftKeyDown());
        final var result = Swaps.swap(input, player, pos);
        if (result.isEmpty()) {
            return InteractionResult.PASS;
        }

        final var swapped = result.get().state();
        if (result.get().lock()) {
            LockedBlocks.lock(level, pos);
        }

        if (swapped != original) {
            level.setBlock(pos, swapped, UPDATE_FLAGS);
            PropSwapEvents.SWAPPED.invoker().onSwapped(player, level, pos, original, swapped);
        }

        player.sendOverlayMessage(result.get().message());
        return InteractionResult.SUCCESS;
    }
}
