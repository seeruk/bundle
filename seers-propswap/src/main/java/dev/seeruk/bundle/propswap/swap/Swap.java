package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Changes one kind of block's state with a tool.
 */
public interface Swap {
    boolean supports(BlockState state);

    /**
     * Returns empty if nothing should happen, leaving it to Vanilla.
     */
    Optional<SwapResult> swap(SwapInput input);
}
