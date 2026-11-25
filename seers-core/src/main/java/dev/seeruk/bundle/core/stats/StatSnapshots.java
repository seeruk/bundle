package dev.seeruk.bundle.core.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.SeersCore;
import dev.seeruk.bundle.core.storage.WorldStorage;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Saved copies of every player's stats, from {@link AllPlayerStats#read}, each standing for a
 * moment, e.g. when this month started. Comparing one with current stats (see {@link StatChanges})
 * says what's happened since.
 *
 * <p>Snapshots are kept for as long as something holds them. A holder is anything that needs one,
 * named so it's unique, e.g. {@code seers-leaders:monthly}. Holders of the same moment share one
 * snapshot, and a snapshot nothing holds any more is deleted.
 */
public final class StatSnapshots {
    private static final Codec<Instant> INSTANT_CODEC = Codec.STRING.comapFlatMap(
        instant -> {
            try {
                return DataResult.success(Instant.parse(instant));
            } catch (DateTimeParseException e) {
                return DataResult.error(() -> "Not a moment in time: " + instant);
            }
        },
        Instant::toString
    );

    private static final Codec<Map<UUID, Map<String, Integer>>> VALUES_CODEC = Codec.unboundedMap(
        UUIDUtil.STRING_CODEC,
        Codec.unboundedMap(Codec.STRING, Codec.INT)
    );

    /**
     * @param holders   The moment each holder holds a snapshot of.
     * @param snapshots Every player's stats, by the moment they stand for.
     */
    private record Stored(Map<String, Instant> holders, Map<Instant, Map<UUID, Map<String, Integer>>> snapshots) {
        private static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, INSTANT_CODEC).fieldOf("holders").forGetter(Stored::holders),
            Codec.unboundedMap(INSTANT_CODEC, VALUES_CODEC).fieldOf("snapshots").forGetter(Stored::snapshots)
        ).apply(instance, Stored::new));
    }

    private static final WorldStorage<Stored> STORED = WorldStorage.create(
        SeersCore.MOD_ID, "stat_snapshots", Stored.CODEC, () -> new Stored(Map.of(), Map.of())
    );

    /**
     * Every player's stats, as they were at a moment.
     *
     * @param at     The moment they stand for.
     * @param values Each player's stat values, by UUID.
     */
    public record Snapshot(Instant at, Map<UUID, Map<String, Integer>> values) {}

    private StatSnapshots() {}

    /**
     * Makes the holder hold a snapshot standing for {@code at}, instead of any it held before. If
     * there isn't one for that moment yet, it's taken from players' current stats.
     */
    public static void hold(MinecraftServer server, String holder, Instant at) {
        final var stored = STORED.get(server);
        final var holders = new HashMap<>(stored.holders());
        holders.put(holder, at);

        final var snapshots = new HashMap<>(stored.snapshots());
        snapshots.computeIfAbsent(at, ignored -> copyOf(AllPlayerStats.read(server)));
        STORED.set(server, pruned(holders, snapshots));
    }

    /**
     * The snapshot the holder holds, if any.
     */
    public static Optional<Snapshot> held(MinecraftServer server, String holder) {
        final var stored = STORED.get(server);
        return Optional.ofNullable(stored.holders().get(holder))
            .map(at -> new Snapshot(at, stored.snapshots().get(at)));
    }

    /**
     * Stops the holder holding a snapshot, e.g. when what it was for has been removed.
     */
    public static void release(MinecraftServer server, String holder) {
        final var stored = STORED.get(server);
        if (stored.holders().containsKey(holder)) {
            final var holders = new HashMap<>(stored.holders());
            holders.remove(holder);
            STORED.set(server, pruned(holders, stored.snapshots()));
        }
    }

    // Leaves out snapshots that nothing holds
    private static Stored pruned(Map<String, Instant> holders, Map<Instant, Map<UUID, Map<String, Integer>>> snapshots) {
        final var held = Map.copyOf(snapshots.entrySet().stream()
            .filter(entry -> holders.containsValue(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return new Stored(Map.copyOf(holders), held);
    }

    private static Map<UUID, Map<String, Integer>> copyOf(Map<UUID, Map<String, Integer>> values) {
        return Map.copyOf(values.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue()))));
    }
}
