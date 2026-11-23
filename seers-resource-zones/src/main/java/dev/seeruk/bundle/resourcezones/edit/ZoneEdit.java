package dev.seeruk.bundle.resourcezones.edit;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An immutable, uncommitted edit to resource zones, i.e. the chunks a player has marked to be added
 * to or removed from zones, grouped by dimension.
 *
 * @param mode    What happens to the chunks the player flies over.
 * @param pending Each dimension's changed chunks, each mapped to whether it's added (or removed).
 */
public record ZoneEdit(EditMode mode, Map<ResourceKey<Level>, Map<ChunkPos, Boolean>> pending) {
    public ZoneEdit {
        final var copied = new HashMap<ResourceKey<Level>, Map<ChunkPos, Boolean>>();
        pending.forEach((dimension, changes) -> copied.put(dimension, Map.copyOf(changes)));
        pending = Map.copyOf(copied);
    }

    /**
     * A new edit with no changes yet.
     */
    public static ZoneEdit start(EditMode mode) {
        return new ZoneEdit(mode, Map.of());
    }

    public ZoneEdit withMode(EditMode mode) {
        return new ZoneEdit(mode, pending);
    }

    /**
     * Marks the chunk as the mode says. Marking a chunk as what it already is (e.g. adding a chunk
     * that's already in a zone) drops any change to it, so flying back over a chunk in the other
     * mode undoes it. While paused, nothing changes.
     *
     * @param dimension The dimension the chunk's in.
     * @param chunk     The chunk to mark.
     * @param saved     The dimension's chunks that are in zones now.
     */
    public ZoneEdit mark(ResourceKey<Level> dimension, ChunkPos chunk, Set<ChunkPos> saved) {
        if (mode == EditMode.PAUSED) {
            return this;
        }

        final var adding = mode == EditMode.ADD;
        final var changes = new HashMap<>(in(dimension));
        if (saved.contains(chunk) == adding) {
            changes.remove(chunk);
        } else {
            changes.put(chunk, adding);
        }

        final var updated = new HashMap<>(pending);
        if (changes.isEmpty()) {
            updated.remove(dimension);
        } else {
            updated.put(dimension, changes);
        }

        return new ZoneEdit(mode, updated);
    }

    /**
     * The dimension's changed chunks, each mapped to whether it's added (or removed).
     */
    public Map<ChunkPos, Boolean> in(ResourceKey<Level> dimension) {
        return pending.getOrDefault(dimension, Map.of());
    }

    public Set<ChunkPos> added(ResourceKey<Level> dimension) {
        return matching(dimension, true);
    }

    public Set<ChunkPos> removed(ResourceKey<Level> dimension) {
        return matching(dimension, false);
    }

    /**
     * How many chunks are added, in every dimension.
     */
    public long added() {
        return count(true);
    }

    /**
     * How many chunks are removed, in every dimension.
     */
    public long removed() {
        return count(false);
    }

    private Set<ChunkPos> matching(ResourceKey<Level> dimension, boolean added) {
        return in(dimension).entrySet().stream()
            .filter(change -> change.getValue() == added)
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());
    }

    private long count(boolean added) {
        return pending.values().stream()
            .flatMap(changes -> changes.values().stream())
            .filter(change -> change == added)
            .count();
    }
}
