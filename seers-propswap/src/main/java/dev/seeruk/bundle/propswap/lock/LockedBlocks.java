package dev.seeruk.bundle.propswap.lock;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.propswap.SeersPropSwap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Locks cleared this tick. Vanilla removes a broken block before working out its drops, so this
    // is the only way drop logic can tell a broken block was locked. Concurrent, as some mods tick
    // each dimension on its own thread.
    private static final Map<ResourceKey<Level>, LongSet> RECENTLY_UNLOCKED = new ConcurrentHashMap<>();

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

    /**
     * Whether the position is locked, or was until earlier this tick.
     */
    public static boolean wasLocked(Level level, BlockPos pos) {
        if (isLocked(level, pos)) {
            return true;
        }

        final var positions = RECENTLY_UNLOCKED.get(level.dimension());
        return positions != null && positions.contains(pos.asLong());
    }

    public static void lock(Level level, BlockPos pos) {
        final var chunk = level.getChunkAt(pos);
        if (chunk.getAttachedOrCreate(ATTACHMENT, LongOpenHashSet::new).add(pos.asLong())) {
            chunk.markUnsaved();
        }
    }

    public static void unlock(Level level, BlockPos pos) {
        unlock(level.getChunkAt(pos), pos);
    }

    /**
     * Clears the position's lock if its block is being replaced by a different block. Otherwise, it
     * forgets any lock cleared there earlier this tick, so a new block can't be mistaken for a
     * locked one.
     */
    public static void blockChanging(LevelChunk chunk, BlockPos pos, BlockState state) {
        if (chunk.getLevel().isClientSide()) {
            return;
        }

        if (isLocked(chunk, pos)) {
            if (!chunk.getBlockState(pos).is(state.getBlock())) {
                unlock(chunk, pos);
            }

            return;
        }

        final var positions = RECENTLY_UNLOCKED.get(chunk.getLevel().dimension());
        if (positions != null) {
            positions.remove(pos.asLong());
        }
    }

    public static void forgetRecentlyUnlocked() {
        RECENTLY_UNLOCKED.clear();
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
        RECENTLY_UNLOCKED.computeIfAbsent(chunk.getLevel().dimension(), key -> new LongOpenHashSet()).add(pos.asLong());
    }
}
