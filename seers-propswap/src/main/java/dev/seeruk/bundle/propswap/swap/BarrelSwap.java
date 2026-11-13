package dev.seeruk.bundle.propswap.swap;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Rotates barrels, or toggles whether they're forced open. A forced open barrel is locked, so
 * Vanilla doesn't close it again.
 */
public final class BarrelSwap implements PlayerAwareSwap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof BarrelBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input, Player player, BlockPos pos) {
        if (!input.crouching()) {
            return Optional.of(rotate(input));
        }

        final var level = player.level();
        final var forceOpen = !LockedBlocks.isLocked(level, pos);
        if (!forceOpen) {
            LockedBlocks.unlock(level, pos);
        }

        // A barrel that's no longer forced open stays open while someone's still using it
        final var inUse = level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel
            && !barrel.getEntitiesWithContainerOpen().isEmpty();

        final var state = input.state().setValue(BarrelBlock.OPEN, forceOpen || inUse);
        final var result = SwapResult.changed(state, "forced open", String.valueOf(forceOpen));

        return Optional.of(forceOpen ? result.locked() : result);
    }

    /**
     * Turns the barrel to face the clicked side, or the opposite way if it already does.
     */
    static SwapResult rotate(SwapInput input) {
        final var current = input.state().getValue(BarrelBlock.FACING);
        final var facing = current == input.face() ? current.getOpposite() : input.face();

        return SwapResult.changed(input.state().setValue(BarrelBlock.FACING, facing), BarrelBlock.FACING);
    }
}
