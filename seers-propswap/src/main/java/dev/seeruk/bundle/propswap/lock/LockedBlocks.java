package dev.seeruk.bundle.propswap.lock;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.propswap.SeersPropSwap;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
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

    private static final StreamCodec<ByteBuf, LongSet> SYNC_CODEC = ByteBufCodecs.LONG_ARRAY.map(
        LongOpenHashSet::new,
        LongSet::toLongArray
    );

    // Synced to clients with PropSwap installed, so their own predictions keep locked blocks too.
    // Fabric only sends it to clients that know about it, so Vanilla clients are unaffected.
    public static final AttachmentType<LongSet> ATTACHMENT = AttachmentRegistry.create(
        SeersPropSwap.id("locked_blocks"),
        builder -> builder.persistent(CODEC).syncWith(SYNC_CODEC, AttachmentSyncPredicate.all())
    );

    // Locks cleared this tick. Vanilla removes a broken block before working out its drops, so this
    // is the only way drop logic can tell a broken block was locked. Concurrent, as some mods tick
    // each dimension on its own thread.
    private static final Map<ResourceKey<Level>, LongSet> RECENTLY_UNLOCKED = new ConcurrentHashMap<>();

    private LockedBlocks() {}

    // Only here to make sure the class (and its attachment) is registered on mod initialization.
    public static void init() {}

    public static boolean isLocked(LevelReader level, BlockPos pos) {
        // Anything other than a level (e.g. world generation) never has locks
        if (!(level instanceof Level fullLevel)) {
            return false;
        }

        final var chunk = fullLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
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

    // Fabric only syncs an attachment when it's set again, so locking and unlocking replace the set
    // rather than changing it.
    public static void lock(Level level, BlockPos pos) {
        final var chunk = level.getChunkAt(pos);
        final var positions = chunk.getAttached(ATTACHMENT);
        if (positions != null && positions.contains(pos.asLong())) {
            return;
        }

        final var updated = positions == null ? new LongOpenHashSet() : new LongOpenHashSet(positions.toLongArray());
        updated.add(pos.asLong());

        chunk.setAttached(ATTACHMENT, updated);
        chunk.markUnsaved();
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
        if (positions == null || !positions.contains(pos.asLong())) {
            return;
        }

        final var updated = new LongOpenHashSet(positions.toLongArray());
        updated.remove(pos.asLong());

        if (updated.isEmpty()) {
            chunk.removeAttached(ATTACHMENT);
        } else {
            chunk.setAttached(ATTACHMENT, updated);
        }

        chunk.markUnsaved();
        RECENTLY_UNLOCKED.computeIfAbsent(chunk.getLevel().dimension(), key -> new LongOpenHashSet()).add(pos.asLong());
    }
}
