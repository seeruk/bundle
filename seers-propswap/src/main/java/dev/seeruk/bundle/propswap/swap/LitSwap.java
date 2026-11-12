package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Toggles whether a furnace looks lit. It stays that way until the furnace is actually used.
 */
public final class LitSwap implements Swap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof AbstractFurnaceBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input) {
        final var state = input.state().cycle(AbstractFurnaceBlock.LIT);
        return Optional.of(SwapResult.changed(state, AbstractFurnaceBlock.LIT));
    }
}
