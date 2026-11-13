package dev.seeruk.bundle.propswap.swap;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;

import java.util.Optional;

/**
 * Cycles the sides of walls, or toggles their post when crouching on a side. A wall always keeps
 * its post if it would otherwise have nothing, as an empty wall is invisible.
 */
public final class WallSwap implements Swap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof WallBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input) {
        final var state = input.state();
        final var vertical = input.face().getAxis().isVertical();

        if (input.crouching()) {
            return Optional.of(vertical ? toggleAll(state).locked() : togglePost(state));
        }

        // Clicking the top or bottom cycles the side the player is looking towards
        final var side = WallBlock.PROPERTY_BY_DIRECTION.get(vertical ? input.facing() : input.face());
        final var result = SwapResult.cycled(state, side, false);
        if (isEmpty(result.state())) {
            return Optional.of(SwapResult.changed(result.state().setValue(WallBlock.UP, true), side).locked());
        }

        return Optional.of(result.locked());
    }

    private static SwapResult togglePost(BlockState state) {
        final var result = SwapResult.cycled(state, WallBlock.UP, false);
        if (isEmpty(result.state())) {
            return new SwapResult(state, Component.literal("A wall needs a post or at least one side"));
        }

        return result.locked();
    }

    private static SwapResult toggleAll(BlockState state) {
        final var sides = WallBlock.PROPERTY_BY_DIRECTION.values();
        final var value = sides.stream().allMatch(side -> state.getValue(side) == WallSide.NONE) ? WallSide.LOW : WallSide.NONE;

        var updated = state;
        for (final var side : sides) {
            updated = updated.setValue(side, value);
        }

        if (isEmpty(updated)) {
            updated = updated.setValue(WallBlock.UP, true);
        }

        return SwapResult.changed(updated, "sides", value.getSerializedName());
    }

    private static boolean isEmpty(BlockState state) {
        return !state.getValue(WallBlock.UP)
            && WallBlock.PROPERTY_BY_DIRECTION.values().stream().allMatch(side -> state.getValue(side) == WallSide.NONE);
    }
}
