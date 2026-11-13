package dev.seeruk.bundle.propswap.mixin;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clears a lock when its block is replaced, however that happens (breaking, pistons, explosions,
 * commands, etc.).
 */
@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void propswap$clearReplacedLock(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
        LockedBlocks.blockChanging((LevelChunk) (Object) this, pos, state);
    }
}
