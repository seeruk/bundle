package dev.seeruk.bundle.propswap.swap;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.Optional;

/**
 * Splits beds, then flips either half between head and foot.
 *
 * <p>Once split, each half is worth exactly one bed. Splitting costs a bed of the same kind (unless
 * the player is in creative), and each split half drops one bed when broken, even a foot. That way
 * beds can't be duplicated or lost.
 */
public final class BedSwap implements PlayerAwareSwap {
    @Override
    public boolean supports(BlockState state) {
        return state.getBlock() instanceof AbstractBedBlock;
    }

    @Override
    public Optional<SwapResult> swap(SwapInput input, Player player, BlockPos pos) {
        final var level = player.level();
        if (LockedBlocks.isLocked(level, pos)) {
            return Optional.of(flip(input.state()));
        }

        final var state = input.state();
        final var bed = state.getBlock().asItem();
        if (!player.hasInfiniteMaterials() && !take(player, bed)) {
            final var name = new ItemStack(bed).getHoverName();
            return Optional.of(new SwapResult(
                state,
                Component.literal("You need a ").append(name).append(" to split this")
            ));
        }

        final var partner = pos.relative(AbstractBedBlock.getConnectedDirection(state));
        if (level.getBlockState(partner).is(state.getBlock())) {
            LockedBlocks.lock(level, partner);
        }

        return Optional.of(new SwapResult(state, Component.literal("Bed split")).locked());
    }

    /**
     * Turns a head into a foot or a foot into a head, facing the other way so it still lines up.
     */
    static SwapResult flip(BlockState state) {
        final var part = state.getValue(AbstractBedBlock.PART) == BedPart.HEAD ? BedPart.FOOT : BedPart.HEAD;
        final var flipped = state
            .setValue(AbstractBedBlock.PART, part)
            .setValue(AbstractBedBlock.FACING, state.getValue(AbstractBedBlock.FACING).getOpposite());

        return SwapResult.changed(flipped, AbstractBedBlock.PART).locked();
    }

    /**
     * The state a broken block's drops should come from. Vanilla only drops a bed from a head, so a
     * split foot drops what a head would.
     */
    public static BlockState dropState(BlockState state, Level level, BlockPos pos) {
        if (!(state.getBlock() instanceof AbstractBedBlock) || state.getValue(AbstractBedBlock.PART) != BedPart.FOOT) {
            return state;
        }

        return LockedBlocks.wasLocked(level, pos) ? state.setValue(AbstractBedBlock.PART, BedPart.HEAD) : state;
    }

    private static boolean take(Player player, Item item) {
        final var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            final var stack = inventory.getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }
}
