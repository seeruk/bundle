package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.stats.StatPeriods;
import dev.seeruk.bundle.leaders.LeadersConfig.Period;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.minecraft.server.MinecraftServer;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static dev.seeruk.bundle.leaders.SeersLeaders.LOGGER;
import static dev.seeruk.bundle.leaders.SeersLeaders.MOD_ID;

/**
 * Periods (see {@link Period}): the stretches of time that periodic categories count stats in.
 */
public final class Periods {
    private static final StatPeriods PERIODS = StatPeriods.of(MOD_ID);

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
        return PERIODS.sinceStart(server, period, stats);
    }

    /**
     * Ends any period whose schedule has come round since it started, with its results, and starts
     * the next (see {@link StatPeriods#check}).
     */
    public static void check(MinecraftServer server, LeadersConfig config, Clock clock) {
        var changed = false;
        for (final var period : config.periods()) {
            final var outcome = PERIODS.check(server, period.id(), period.schedule(), clock, ended ->
                end(server, config, period, ended.missedWholePeriod()));
            changed |= outcome != StatPeriods.Outcome.UNCHANGED;
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
        PERIODS.forgetExcept(server, ids).forEach(id ->
            LOGGER.warn("Period '{}' isn't in the config. Any existing snapshots will be released", id));
    }

    // Saves the final results, fires the event, and announces the winners, unless they're stale
    private static void end(MinecraftServer server, LeadersConfig config, Period period, boolean stale) {
        final var winners = Leaders.endPeriod(server, config, period.id());
        final var end = new LeaderEvents.PeriodEnd(period.id(), period.displayTitle(), winners, !winners.isEmpty() && !stale);

        LeaderEvents.PERIOD_ENDED.invoker().onPeriodEnded(server, end);
        LeaderAnnouncements.announcePeriod(server, config, end);
    }
}
