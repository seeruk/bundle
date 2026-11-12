package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Cycles the axis of pillars (logs, basalt, quartz, etc.).
 */
public final class AxisSwap implements Swap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof RotatedPillarBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input) {
        return Optional.of(SwapResult.cycled(input.state(), RotatedPillarBlock.AXIS, input.crouching()));
    }
}
