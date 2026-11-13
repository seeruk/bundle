package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Cycles the shape of stairs, corners included.
 */
public final class StairSwap implements Swap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof StairBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input) {
        return Optional.of(SwapResult.cycled(input.state(), StairBlock.SHAPE, input.crouching()).locked());
    }
}
