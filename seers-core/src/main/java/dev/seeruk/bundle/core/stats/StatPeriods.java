package dev.seeruk.bundle.core.stats;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.core.SeersCore;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.core.time.Schedule;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A mod's periods: stretches of time that stats are counted over, e.g. a week, each ending when its
 * {@link Schedule} comes round, when the next starts straight away. What's happened in a period is
 * players' current stats compared with a snapshot taken when it started (see {@link StatChanges}).
 *
 * <p>Snapshots are shared (see {@link StatSnapshots}), so periods that start at the same moment, in
 * any mod, and whatever they're called, share one.
 */
public final class StatPeriods {
    // The schedule each period was started on, by its full ID (e.g. "seers-leaders:monthly"), so a
    // changed schedule can be noticed. When it started is the moment its snapshot stands for.
    private static final WorldStorage<Map<String, Schedule>> SCHEDULES = WorldStorage.create(
        SeersCore.MOD_ID, "period_schedules", Codec.unboundedMap(Codec.STRING, Schedule.CODEC), Map::of
    );

    private final String modId;

    /**
     * What happened to a period when it was checked.
     */
    public enum Outcome {
        UNCHANGED,
        /** It started, quietly, without ending one before it. */
        STARTED,
        /** It ended, and the next one started. */
        ENDED
    }

    /**
     * A period that's ended.
     *
     * @param started           When it started.
     * @param ended             The moment its schedule came round, which it ended at.
     * @param missedWholePeriod Whether its schedule also came round in between, meaning at least
     *                          one whole period passed while the server wasn't running.
     */
    public record Ended(Instant started, Instant ended, boolean missedWholePeriod) {}

    private StatPeriods(String modId) {
        this.modId = modId;
    }

    /**
     * The mod's periods, kept apart from every other mod's.
     */
    public static StatPeriods of(String modId) {
        return new StatPeriods(modId);
    }

    /**
     * Ends the period if its schedule has come round since it started, and starts the next.
     * However many times the schedule has come round, it only ends once, as stats can't change
     * while the server isn't running. A period whose start can't be trusted (it never started, its
     * schedule has changed, or the clock went back past it) is started again quietly instead, so
     * the first one is shorter.
     *
     * @param server   The server.
     * @param id       The period's ID, within the mod.
     * @param schedule When it ends.
     * @param now      The time now, in the schedule's time zone.
     * @param ending   Given the period when it ends, while its stats can still be counted with
     *                 {@link #sinceStart}, before the next one starts.
     */
    public Outcome check(
        MinecraftServer server,
        String id,
        Schedule schedule,
        ZonedDateTime now,
        Consumer<Ended> ending
    ) {
        // The moment the period would have started at, if it had started on time
        final var latest = schedule.latest(now);
        final var started = StatSnapshots.held(server, holder(id)).map(StatSnapshots.Snapshot::at);

        if (started.isEmpty()
            || !schedule.equals(SCHEDULES.get(server).get(holder(id)))
            || started.get().isAfter(now.toInstant())) {
            start(server, id, schedule, latest);
            return Outcome.STARTED;
        }

        if (!latest.isAfter(started.get())) {
            return Outcome.UNCHANGED;
        }

        ending.accept(new Ended(started.get(), latest, missedWholePeriod(schedule, started.get(), latest, now)));
        start(server, id, schedule, latest);
        return Outcome.ENDED;
    }

    /**
     * Each player's stats, counting only what's happened since the period started. Nobody has any
     * until it has started.
     */
    public Map<UUID, Map<String, Integer>> sinceStart(
        MinecraftServer server,
        String id,
        Map<UUID, Map<String, Integer>> stats
    ) {
        return StatSnapshots.held(server, holder(id))
            .map(snapshot -> StatChanges.since(stats, snapshot.values()))
            .orElse(Map.of());
    }

    /**
     * Forgets the mod's periods that aren't given, e.g. ones removed from its config, so their
     * snapshots can be deleted. Returns the IDs of those it forgot.
     */
    public List<String> forgetExcept(MinecraftServer server, Collection<String> ids) {
        final var prefix = modId + ":";
        final var forgotten = SCHEDULES.get(server).keySet().stream()
            .filter(holder -> holder.startsWith(prefix))
            .map(holder -> holder.substring(prefix.length()))
            .filter(id -> !ids.contains(id))
            .toList();
        if (forgotten.isEmpty()) {
            return forgotten;
        }

        forgotten.forEach(id -> StatSnapshots.release(server, holder(id)));
        SCHEDULES.update(server, schedules -> {
            final var kept = new HashMap<>(schedules);
            forgotten.forEach(id -> kept.remove(holder(id)));
            return Map.copyOf(kept);
        });
        return forgotten;
    }

    // Whether the schedule also came round between `started` and `latest`. The schedule's moment
    // just before `latest` is normally `started` itself.
    private static boolean missedWholePeriod(Schedule schedule, Instant started, Instant latest, ZonedDateTime now) {
        final var before = schedule.latest(ZonedDateTime.ofInstant(latest.minusSeconds(1), now.getZone()));
        return before.isAfter(started);
    }

    private void start(MinecraftServer server, String id, Schedule schedule, Instant at) {
        StatSnapshots.hold(server, holder(id), at);
        SCHEDULES.update(server, schedules -> {
            final var updated = new HashMap<>(schedules);
            updated.put(holder(id), schedule);
            return Map.copyOf(updated);
        });
    }

    // The period's full ID, which is also what holds its snapshot
    private String holder(String id) {
        return modId + ":" + id;
    }
}
