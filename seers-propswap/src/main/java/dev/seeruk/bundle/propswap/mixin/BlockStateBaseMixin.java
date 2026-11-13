package dev.seeruk.bundle.propswap.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import dev.seeruk.bundle.propswap.swap.BedSwap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Keeps a locked block's state when a neighbour changes, and makes split bed halves drop a bed.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
    @Shadow
    protected abstract BlockState asState();

    // Vanilla's update still runs, so its other effects (e.g. scheduling fluid ticks) still happen.
    // If the block is locked, its new state is thrown away, even if it would be removed (e.g. one
    // half of a split bed when the other half is broken).
    @ModifyReturnValue(method = "updateShape", at = @At("RETURN"))
    private BlockState propswap$keepLockedShape(BlockState updated, LevelReader level, ScheduledTickAccess ticks, BlockPos pos) {
        return LockedBlocks.isLocked(level, pos) ? asState() : updated;
    }

    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private void propswap$dropSplitBeds(LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        final var state = asState();
        final var origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (!(state.getBlock() instanceof AbstractBedBlock) || origin == null) {
            return;
        }

        final var dropState = BedSwap.dropState(state, builder.getLevel(), BlockPos.containing(origin));
        if (dropState != state) {
            cir.setReturnValue(dropState.getDrops(builder));
        }
    }
}
