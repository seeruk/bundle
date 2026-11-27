package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.core.stats.StatPeriods;
import dev.seeruk.bundle.digest.api.DigestEvents;
import dev.seeruk.bundle.digest.api.Issue;
import net.minecraft.server.MinecraftServer;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static dev.seeruk.bundle.digest.SeersDigest.MOD_ID;

/**
 * The schedule issues are published on. Each issue covers everything since the one before it,
 * kept as a period (see {@link StatPeriods}) that ends when the issue is published.
 */
public final class IssueSchedule {
    // The period the current issue covers. Tests use their own, so they don't affect it.
    static final String ISSUE = "issue";

    private static final StatPeriods PERIODS = StatPeriods.of(MOD_ID);

    private IssueSchedule() {}

    /**
     * Forgets periods other than the current issue's, e.g. ones tests used, so their snapshots can
     * be deleted.
     */
    public static void forgetOldPeriods(MinecraftServer server) {
        PERIODS.forgetExcept(server, List.of(ISSUE));
    }

    /**
     * Publishes the current issue if the schedule has come round since it started (see
     * {@link StatPeriods#endIfDue}).
     */
    public static void publishIfDue(MinecraftServer server, DigestConfig config, Clock clock) {
        publishIfDue(server, config, ISSUE, clock);
    }

    /**
     * Publishes the current issue now, rather than when the schedule says, if anyone's been active
     * since the last one. The next issue then covers from now.
     */
    public static Optional<Issue> publishNow(MinecraftServer server, DigestConfig config, Clock clock) {
        return publishNow(server, config, ISSUE, clock);
    }

    static StatPeriods.Outcome publishIfDue(MinecraftServer server, DigestConfig config, String period, Clock clock) {
        final var outcome = PERIODS.endIfDue(
            server, period, config.schedule(), clock, ended ->
                publish(server, config, period, ended)
        );
        forgetMomentsIfStarted(server, outcome);
        return outcome;
    }

    static Optional<Issue> publishNow(MinecraftServer server, DigestConfig config, String period, Clock clock) {
        // The issue is made while the period ends, before the next starts, as that's when its stats
        // can still be counted (see StatPeriods#endNow). It's kept here to be returned afterwards.
        final var published = new AtomicReference<Issue>();
        final var outcome = PERIODS.endNow(
            server, period, config.schedule(), clock, ended ->
                publish(server, config, period, ended).ifPresent(published::set)
        );
        forgetMomentsIfStarted(server, outcome);
        return Optional.ofNullable(published.get());
    }

    // A new issue has started if the last ended, or if it started without one before it. Either
    // way, it only counts stats from now, so it only has moments from now too.
    private static void forgetMomentsIfStarted(MinecraftServer server, StatPeriods.Outcome outcome) {
        if (outcome != StatPeriods.Outcome.UNCHANGED) {
            Moments.forget(server);
        }
    }

    // Makes the issue, then archives it and fires the event, unless nobody was active
    private static Optional<Issue> publish(
        MinecraftServer server,
        DigestConfig config,
        String period,
        StatPeriods.Ended ended
    ) {
        final var issue = Issues.build(
            Archive.nextIssueNumber(server),
            ended.started(),
            ended.ended(),
            config.groups(),
            PERIODS.sinceStart(server, period, AllPlayerStats.read(server)),
            PERIODS.playersAtStart(server, period),
            Moments.pending(server)
        );

        issue.ifPresent(found -> {
            Archive.add(server, found);
            DigestEvents.PUBLISHED.invoker().onPublished(server, found);
        });
        return issue;
    }
}
