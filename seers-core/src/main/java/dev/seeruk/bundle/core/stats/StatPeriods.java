package dev.seeruk.bundle.core.stats;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.core.SeersCore;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.core.time.Schedule;
import net.minecraft.server.MinecraftServer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
     * schedule has changed, or the clock went back past it) is started again quietly instead, from
     * now, so the first one is shorter.
     *
     * @param server   The server.
     * @param id       The period's ID, within the mod.
     * @param schedule When it ends.
     * @param clock    Says what time it is, in the schedule's time zone.
     * @param ending   Given the period when it ends, while its stats can still be counted with
     *                 {@link #sinceStart}, before the next one starts.
     */
    public Outcome check(
        MinecraftServer server,
        String id,
        Schedule schedule,
        Clock clock,
        Consumer<Ended> ending
    ) {
        // Read once, so every part of the check agrees on what time it is
        final var now = ZonedDateTime.now(clock);

        // The last time the schedule came round (at or before now). If the period started before
        // this, the schedule's come round since, so it ends here.
        final var latest = schedule.latestAtOrBefore(now);
        final var started = StatSnapshots.held(server, holder(id)).map(StatSnapshots.Snapshot::at);

        // The new period is started from now, as that's when its stats are counted from. Starting
        // it from the schedule's last moment would claim it covered time that it didn't cover.
        if (started.isEmpty()
            || !schedule.equals(SCHEDULES.get(server).get(holder(id)))
            || started.get().isAfter(now.toInstant())) {
            start(server, id, schedule, secondOf(now));
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
     * Ends the period now, whatever its schedule says, and starts the next straight away, which
     * then ends when the schedule next comes round. Starts it instead, if it hasn't started.
     *
     * @param server   The server.
     * @param id       The period's ID, within the mod.
     * @param schedule When it normally ends.
     * @param clock    Says what time it is, which it ends at, in the schedule's time zone.
     * @param ending   Given the period, before the next one starts, as with {@link #check}.
     */
    public Outcome endNow(
        MinecraftServer server,
        String id,
        Schedule schedule,
        Clock clock,
        Consumer<Ended> ending
    ) {
        final var now = ZonedDateTime.now(clock);
        final var started = StatSnapshots.held(server, holder(id)).map(StatSnapshots.Snapshot::at);
        if (started.isEmpty()) {
            start(server, id, schedule, secondOf(now));
            return Outcome.STARTED;
        }

        ending.accept(new Ended(started.get(), secondOf(now), false));
        start(server, id, schedule, secondOf(now));
        return Outcome.ENDED;
    }

    /**
     * The players who had stats when the period started, e.g. so players new since then can be
     * told apart. Nobody, until it has started.
     */
    public Set<UUID> playersAtStart(MinecraftServer server, String id) {
        return StatSnapshots.held(server, holder(id))
            .map(snapshot -> snapshot.values().keySet())
            .orElse(Set.of());
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
        SCHEDULES.update(
            server, schedules -> {
                final var kept = new HashMap<>(schedules);
                forgotten.forEach(id -> kept.remove(holder(id)));
                return Map.copyOf(kept);
            }
        );
        return forgotten;
    }

    // The second it is now. Periods started in the same second (e.g. by different mods, as the
    // server starts) share a snapshot, which they wouldn't if they started a few nanoseconds apart.
    private static Instant secondOf(ZonedDateTime now) {
        return now.toInstant().truncatedTo(ChronoUnit.SECONDS);
    }

    // Whether the schedule also came round between `started` and `latest`. The schedule's moment
    // just before `latest` is normally `started` itself.
    private static boolean missedWholePeriod(Schedule schedule, Instant started, Instant latest, ZonedDateTime now) {
        final var before = schedule.latestAtOrBefore(ZonedDateTime.ofInstant(latest.minusSeconds(1), now.getZone()));
        return before.isAfter(started);
    }

    private void start(MinecraftServer server, String id, Schedule schedule, Instant at) {
        StatSnapshots.hold(server, holder(id), at);
        SCHEDULES.update(
            server, schedules -> {
                final var updated = new HashMap<>(schedules);
                updated.put(holder(id), schedule);
                return Map.copyOf(updated);
            }
        );
    }

    // The period's full ID, which is also what holds its snapshot
    private String holder(String id) {
        return modId + ":" + id;
    }
}
