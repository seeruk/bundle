package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

/**
 * Every supported {@link Swap}.
 */
public final class Swaps {
    private static final List<Swap> ALL = List.of(
        new AxisSwap(),
        new LitSwap(),
        new MushroomSwap(),
        new StairSwap()
    );

    private Swaps() {}

    public static Optional<Swap> find(BlockState state) {
        return ALL.stream().filter(swap -> swap.supports(state)).findFirst();
    }
}
