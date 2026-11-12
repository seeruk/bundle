package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Toggles the clicked face of a mushroom block or stem between pores and skin.
 */
public final class MushroomSwap implements Swap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof HugeMushroomBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input) {
        final var property = PipeBlock.PROPERTY_BY_DIRECTION.get(input.face());
        return Optional.of(SwapResult.cycled(input.state(), property, false));
    }
}
