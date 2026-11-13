package dev.seeruk.bundle.propswap.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps a locked block's state when a neighbour changes.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
    @Shadow
    protected abstract BlockState asState();

    // Vanilla's update still runs, so its other effects (e.g. scheduling fluid ticks) still happen.
    // If the block is locked, its new state is thrown away, unless the block is being replaced
    // (e.g. it can't survive).
    @ModifyReturnValue(method = "updateShape", at = @At("RETURN"))
    private BlockState propswap$keepLockedShape(BlockState updated, LevelReader level, ScheduledTickAccess ticks, BlockPos pos) {
        if (updated.is(asState().getBlock()) && LockedBlocks.isLocked(level, pos)) {
            return asState();
        }

        return updated;
    }
}
