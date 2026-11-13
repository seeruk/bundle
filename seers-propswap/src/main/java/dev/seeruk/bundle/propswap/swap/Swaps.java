package dev.seeruk.bundle.propswap.swap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Every supported {@link Swap} and {@link PlayerAwareSwap}.
 */
public final class Swaps {
    private static final List<Swap> SWAPS = List.of(
        new AxisSwap(),
        new ConnectionSwap(),
        new LitSwap(),
        new MushroomSwap(),
        new StairSwap(),
        new WallSwap()
    );

    private static final List<PlayerAwareSwap> PLAYER_AWARE_SWAPS = List.of(
        new BarrelSwap(),
        new BedSwap()
    );

    private Swaps() {}

    public static boolean supports(BlockState state) {
        return PLAYER_AWARE_SWAPS.stream().anyMatch(swap -> swap.supports(state))
            || SWAPS.stream().anyMatch(swap -> swap.supports(state));
    }

    /**
     * Swaps the block with whichever swap supports it. Returns empty if none do, or the swap
     * decided nothing should happen.
     */
    public static Optional<SwapResult> swap(SwapInput input, Player player, BlockPos pos) {
        for (final var swap : PLAYER_AWARE_SWAPS) {
            if (swap.supports(input.state())) {
                return swap.swap(input, player, pos);
            }
        }

        for (final var swap : SWAPS) {
            if (swap.supports(input.state())) {
                return swap.swap(input);
            }
        }

        return Optional.empty();
    }
}
