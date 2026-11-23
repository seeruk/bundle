package dev.seeruk.bundle.resourcezones;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;

/**
 * An immutable collection of the chunks that are in resource zones, grouped by dimension.
 */
public record ZoneChunks(Map<ResourceKey<Level>, Set<ChunkPos>> chunks) {
    public static final ZoneChunks EMPTY = new ZoneChunks(Map.of());

    public static final Codec<ZoneChunks> CODEC = Codec.unboundedMap(
        Level.RESOURCE_KEY_CODEC,
        ChunkPos.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
    ).xmap(ZoneChunks::new, ZoneChunks::chunks);

    public ZoneChunks {
        final var copied = new HashMap<ResourceKey<Level>, Set<ChunkPos>>();
        chunks.forEach((dimension, positions) -> copied.put(dimension, Set.copyOf(positions)));
        chunks = Map.copyOf(copied);
    }

    public boolean contains(ResourceKey<Level> dimension, ChunkPos pos) {
        return in(dimension).contains(pos);
    }

    public Set<ChunkPos> in(ResourceKey<Level> dimension) {
        return chunks.getOrDefault(dimension, Set.of());
    }

    public ZoneChunks with(ResourceKey<Level> dimension, ChunkPos pos) {
        return changed(dimension, positions -> positions.add(pos));
    }

    public ZoneChunks without(ResourceKey<Level> dimension, ChunkPos pos) {
        return changed(dimension, positions -> positions.remove(pos));
    }

    public ZoneChunks withChanges(ResourceKey<Level> dimension, Set<ChunkPos> added, Set<ChunkPos> removed) {
        return changed(
            dimension, positions -> {
                positions.addAll(added);
                positions.removeAll(removed);
            }
        );
    }

    /**
     * Returns a copy of the updated collection, with the given changes applied to the given
     * dimension's chunks. If there are no chunks left after the change, the dimension is removed
     * from being tracked.
     */
    private ZoneChunks changed(ResourceKey<Level> dimension, Consumer<Set<ChunkPos>> change) {
        final var positions = new HashSet<>(chunks.getOrDefault(dimension, Set.of()));
        change.accept(positions);

        final var updated = new HashMap<>(chunks);
        if (positions.isEmpty()) {
            updated.remove(dimension);
        } else {
            updated.put(dimension, positions);
        }

        return new ZoneChunks(updated);
    }
}
