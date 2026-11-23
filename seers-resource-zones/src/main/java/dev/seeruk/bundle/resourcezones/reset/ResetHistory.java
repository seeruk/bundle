package dev.seeruk.bundle.resourcezones.reset;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.resourcezones.ZoneChunks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An immutable record of resource zone chunks being generated and reset. Resets are counted, and
 * each chunk that's been reset remembers the count it was last reset at, so it can be compared with
 * the count a player last saw (e.g. to tell whether it was reset since they left).
 *
 * @param resets    How many times zones have been reset (i.e. the server started with any reset).
 * @param generated The zone chunks known to have been generated, which can be reset.
 * @param resetAt   Each dimension's chunks that have been reset, each mapped to the reset count it
 *                  was last reset at.
 */
public record ResetHistory(int resets, ZoneChunks generated, Map<ResourceKey<Level>, Map<ChunkPos, Integer>> resetAt) {
    public static final ResetHistory EMPTY = new ResetHistory(0, ZoneChunks.EMPTY, Map.of());

    private static final Codec<Map<ChunkPos, Integer>> RESET_AT_CODEC = RecordCodecBuilder.<Map.Entry<ChunkPos, Integer>>create(
        instance -> instance.group(
            ChunkPos.CODEC.fieldOf("chunk").forGetter(Map.Entry::getKey),
            Codec.INT.fieldOf("reset").forGetter(Map.Entry::getValue)
        ).apply(instance, Map::entry)
    ).listOf().xmap(
        entries -> entries.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b)),
        resetAt -> List.copyOf(resetAt.entrySet())
    );

    public static final Codec<ResetHistory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("resets").forGetter(ResetHistory::resets),
        ZoneChunks.CODEC.fieldOf("generated").forGetter(ResetHistory::generated),
        Codec.unboundedMap(
            Level.RESOURCE_KEY_CODEC,
            RESET_AT_CODEC
        ).fieldOf("reset_at").forGetter(ResetHistory::resetAt)
    ).apply(instance, ResetHistory::new));

    public ResetHistory {
        final var copied = new HashMap<ResourceKey<Level>, Map<ChunkPos, Integer>>();
        resetAt.forEach((dimension, chunks) -> copied.put(dimension, Map.copyOf(chunks)));
        resetAt = Map.copyOf(copied);
    }

    public ResetHistory withGenerated(ResourceKey<Level> dimension, ChunkPos chunk) {
        return new ResetHistory(resets, generated.with(dimension, chunk), resetAt);
    }

    /**
     * Brings the dimension's generated chunks up to date: chunks that exist now are added, and
     * chunks that are no longer in a zone are dropped.
     *
     * @param dimension The dimension.
     * @param zone      The dimension's chunks that are in zones.
     * @param existing  Which of those chunks exist (e.g. are loaded, or stored in region files).
     */
    public ResetHistory withRefreshedGenerated(
        ResourceKey<Level> dimension,
        Set<ChunkPos> zone,
        Set<ChunkPos> existing
    ) {
        final var added = existing.stream().filter(zone::contains).collect(Collectors.toSet());
        final var dropped = generated.in(dimension).stream()
            .filter(chunk -> !zone.contains(chunk))
            .collect(Collectors.toSet());

        return new ResetHistory(resets, generated.withChanges(dimension, added, dropped), resetAt);
    }

    /**
     * The dimension's generated chunks that aren't stored any more, i.e. that have been reset.
     *
     * @param dimension The dimension.
     * @param stored    Which of its generated chunks are stored in region files.
     */
    public Set<ChunkPos> missing(ResourceKey<Level> dimension, Set<ChunkPos> stored) {
        return generated.in(dimension).stream()
            .filter(chunk -> !stored.contains(chunk))
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Records a reset of the given chunks, as one more reset. The chunks are no longer generated,
     * until they're generated again. If no chunks were reset, nothing changes.
     */
    public ResetHistory withReset(Map<ResourceKey<Level>, Set<ChunkPos>> reset) {
        if (reset.values().stream().allMatch(Set::isEmpty)) {
            return this;
        }

        final var count = resets + 1;
        var updatedGenerated = generated;
        final var updatedResetAt = new HashMap<>(resetAt);
        for (final var entry : reset.entrySet()) {
            final var dimension = entry.getKey();
            updatedGenerated = updatedGenerated.withChanges(dimension, Set.of(), entry.getValue());

            final var chunks = new HashMap<>(updatedResetAt.getOrDefault(dimension, Map.of()));
            entry.getValue().forEach(chunk -> chunks.put(chunk, count));
            updatedResetAt.put(dimension, chunks);
        }

        return new ResetHistory(count, updatedGenerated, updatedResetAt);
    }

    /**
     * Whether the chunk's been reset since the given reset count, e.g. the one a player last saw.
     */
    public boolean wasResetSince(ResourceKey<Level> dimension, ChunkPos chunk, int seen) {
        return resetAt.getOrDefault(dimension, Map.of()).getOrDefault(chunk, 0) > seen;
    }
}
