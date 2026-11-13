package dev.seeruk.bundle.propswap.swap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Like a {@link Swap}, for blocks that also need to know about the player using the tool, or the
 * level the block is in (via the player).
 */
public interface PlayerAwareSwap {
    boolean supports(BlockState state);

    /**
     * Returns empty if nothing should happen, leaving it to Vanilla.
     */
    Optional<SwapResult> swap(SwapInput input, Player player, BlockPos pos);
}
