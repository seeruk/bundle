package dev.seeruk.bundle.propswap.mixin;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops Vanilla opening and closing locked (i.e. forced open) barrels. The sounds still play, as
 * Vanilla handles those separately.
 */
@Mixin(BarrelBlockEntity.class)
abstract class BarrelBlockEntityMixin {
    @Inject(method = "updateBlockState", at = @At("HEAD"), cancellable = true)
    private void propswap$keepForcedOpen(BlockState state, boolean open, CallbackInfo ci) {
        final var barrel = (BarrelBlockEntity) (Object) this;
        if (barrel.getLevel() != null && LockedBlocks.isLocked(barrel.getLevel(), barrel.getBlockPos())) {
            ci.cancel();
        }
    }
}
