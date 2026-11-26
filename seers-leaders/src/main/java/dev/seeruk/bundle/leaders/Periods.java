package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.core.stats.StatChanges;
import dev.seeruk.bundle.core.stats.StatSnapshots;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.leaders.LeadersConfig.Period;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.seeruk.bundle.leaders.SeersLeaders.LOGGER;
import static dev.seeruk.bundle.leaders.SeersLeaders.MOD_ID;

/**
 * Periods (see {@link Period}): the stretches of time that periodic categories count stats in.
 */
public final class Periods {
    // The schedule each period was started on, by period ID, so a changed schedule can be noticed.
    // When it started is the moment its snapshot stands for.
    private static final WorldStorage<Map<String, Schedule>> SCHEDULES = WorldStorage.create(
        MOD_ID, "period_schedules", Codec.unboundedMap(Codec.STRING, Schedule.CODEC), Map::of
    );

    private Periods() {}

    /**
     * Each player's stats, counting only what's happened since the period started. Nobody has any
     * until it has started.
     */
    public static Map<UUID, Map<String, Integer>> sinceStart(
        MinecraftServer server,
        String period,
        Map<UUID, Map<String, Integer>> stats
    ) {
        return StatSnapshots.held(server, holder(period))
            .map(snapshot -> StatChanges.since(stats, snapshot.values()))
            .orElse(Map.of());
    }

    /**
     * Ends any period whose schedule has come round since it started, and starts the next. Periods
     * whose start can't be trusted are started again quietly, so the first one is shorter.
     */
    public static void check(MinecraftServer server, LeadersConfig config, ZonedDateTime now) {
        final var schedules = SCHEDULES.get(server);
        var changed = false;

        for (final var period : config.periods()) {
            // The moment the period would have started at, if it had started on time
            final var latest = period.schedule().latest(now);
            final var started = StatSnapshots.held(server, holder(period.id())).map(StatSnapshots.Snapshot::at);

            // Started quietly if it never started, or if when it started can't be trusted (its
            // schedule has changed, or the clock went back past it), so there's no fair end to it
            if (started.isEmpty()
                || !period.schedule().equals(schedules.get(period.id()))
                || started.get().isAfter(now.toInstant())) {
                start(server, period, latest);
                changed = true;
            } else if (latest.isAfter(started.get())) {
                end(server, config, period, missedWholePeriod(period, started.get(), latest, now));
                start(server, period, latest);
                changed = true;
            }
        }

        if (changed) {
            Leaders.refresh(server, config);
        }
    }

    /**
     * Forgets periods that aren't in the config, so their snapshots can be deleted, if present.
     */
    public static void forgetRemoved(MinecraftServer server, LeadersConfig config) {
        final var ids = config.periods().stream().map(Period::id).toList();
        final var removed = SCHEDULES.get(server).keySet().stream().filter(id -> !ids.contains(id)).toList();
        if (removed.isEmpty()) {
            return;
        }

        removed.forEach(id -> {
            LOGGER.warn("Period '{}' isn't in the config. Any existing snapshots will be released", id);
            StatSnapshots.release(server, holder(id));
        });
        SCHEDULES.update(server, schedules -> {
            final var kept = new HashMap<>(schedules);
            removed.forEach(kept::remove);
            return Map.copyOf(kept);
        });
    }

    // Saves the final results, fires the event, and announces the winners, unless they're stale
    private static void end(MinecraftServer server, LeadersConfig config, Period period, boolean stale) {
        final var winners = Leaders.endPeriod(server, config, period.id());
        final var end = new LeaderEvents.PeriodEnd(period.id(), period.displayTitle(), winners, !winners.isEmpty() && !stale);

        LeaderEvents.PERIOD_ENDED.invoker().onPeriodEnded(server, end);
        LeaderAnnouncements.announcePeriod(server, config, end);
    }

    // Whether the schedule also came round between `started` and `latest`, meaning a whole period
    // passed while the server wasn't running. The schedule's moment just before `latest` is
    // normally `started` itself.
    private static boolean missedWholePeriod(Period period, Instant started, Instant latest, ZonedDateTime now) {
        final var before = period.schedule().latest(ZonedDateTime.ofInstant(latest.minusSeconds(1), now.getZone()));
        return before.isAfter(started);
    }

    private static void start(MinecraftServer server, Period period, Instant at) {
        StatSnapshots.hold(server, holder(period.id()), at);
        SCHEDULES.update(server, schedules -> {
            final var updated = new HashMap<>(schedules);
            updated.put(period.id(), period.schedule());
            return Map.copyOf(updated);
        });
    }

    private static String holder(String period) {
        return MOD_ID + ":" + period;
    }
}
