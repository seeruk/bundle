package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Toggles the connections of fences, glass panes and bars.
 */
public final class ConnectionSwap implements Swap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof CrossCollisionBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input) {
        if (input.crouching()) {
            return Optional.of(toggleAll(input.state()));
        }

        // Clicking the top or bottom toggles the side the player is looking towards
        final var side = input.face().getAxis().isHorizontal() ? input.face() : input.facing();
        return Optional.of(SwapResult.cycled(input.state(), CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(side), false));
    }

    private static SwapResult toggleAll(BlockState state) {
        final var sides = CrossCollisionBlock.PROPERTY_BY_DIRECTION.values();
        final var connect = sides.stream().noneMatch(state::getValue);

        var updated = state;
        for (final var side : sides) {
            updated = updated.setValue(side, connect);
        }

        return SwapResult.changed(updated, "sides", String.valueOf(connect));
    }
}
