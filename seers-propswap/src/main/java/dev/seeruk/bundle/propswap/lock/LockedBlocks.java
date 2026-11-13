package dev.seeruk.bundle.propswap.lock;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.propswap.SeersPropSwap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;

/**
 * A per-chunk store of which blocks PropSwap has locked. Locks are only needed for PropSwaps where
 * the affected block would be updated by a neighbouring block. The actual locking logic is handled
 * by the mod's mixins.
 */
public final class LockedBlocks {
    private static final Codec<LongSet> CODEC = Codec.LONG_STREAM.xmap(
        positions -> new LongOpenHashSet(positions.toArray()),
        positions -> Arrays.stream(positions.toLongArray())
    );

    public static final AttachmentType<LongSet> ATTACHMENT = AttachmentRegistry.create(
        SeersPropSwap.id("locked_blocks"),
        builder -> builder.persistent(CODEC)
    );

    private LockedBlocks() {}

    // Only here to make sure the class (and its attachment) is registered on mod initialization.
    public static void init() {}

    public static boolean isLocked(LevelReader level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        final var chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && isLocked(chunk, pos);
    }

    public static boolean isLocked(LevelChunk chunk, BlockPos pos) {
        final var positions = chunk.getAttached(ATTACHMENT);
        return positions != null && positions.contains(pos.asLong());
    }

    public static void lock(Level level, BlockPos pos) {
        final var chunk = level.getChunkAt(pos);
        if (chunk.getAttachedOrCreate(ATTACHMENT, LongOpenHashSet::new).add(pos.asLong())) {
            chunk.markUnsaved();
        }
    }

    /**
     * Clears the position's lock if its block is being replaced by a different block.
     */
    public static void blockChanging(LevelChunk chunk, BlockPos pos, BlockState state) {
        if (!chunk.getLevel().isClientSide() && isLocked(chunk, pos) && !chunk.getBlockState(pos).is(state.getBlock())) {
            unlock(chunk, pos);
        }
    }

    private static void unlock(LevelChunk chunk, BlockPos pos) {
        final var positions = chunk.getAttached(ATTACHMENT);
        if (positions == null || !positions.remove(pos.asLong())) {
            return;
        }

        if (positions.isEmpty()) {
            chunk.removeAttached(ATTACHMENT);
        }

        chunk.markUnsaved();
    }
}
