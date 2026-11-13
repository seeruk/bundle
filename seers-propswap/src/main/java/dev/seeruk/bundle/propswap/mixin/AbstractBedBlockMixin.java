package dev.seeruk.bundle.propswap.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * In creative, Vanilla removes a bed's head when its foot is broken, so the bed doesn't drop. This
 * skips that for locked (i.e. split) beds, so each half can be broken on its own.
 */
@Mixin(AbstractBedBlock.class)
abstract class AbstractBedBlockMixin {
    @ModifyExpressionValue(
        method = "playerWillDestroy",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;preventsBlockDrops()Z")
    )
    private boolean propswap$keepSplitHead(
        boolean preventsDrops,
        @Local(argsOnly = true) Level level,
        @Local(argsOnly = true) BlockPos pos
    ) {
        return preventsDrops && !LockedBlocks.isLocked(level, pos);
    }
}
