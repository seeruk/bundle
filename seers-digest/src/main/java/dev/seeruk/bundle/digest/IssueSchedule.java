package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.core.stats.StatPeriods;
import dev.seeruk.bundle.digest.api.DigestEvents;
import dev.seeruk.bundle.digest.api.Issue;
import net.minecraft.SharedConstants;
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

    public static void started(MinecraftServer server) {
        PERIODS.forgetExcept(server, List.of(ISSUE));
    }

    /**
     * Checks the schedule every second, so issues are published within a second of it. It's only
     * checked from ticks, so an issue due while the server was stopped is only published once every
     * mod has started, and can listen for it.
     */
    public static void tick(MinecraftServer server, DigestConfig config, Clock clock) {
        if (server.getTickCount() % SharedConstants.TICKS_PER_SECOND == 0) {
            check(server, config, ISSUE, clock);
        }
    }

    /**
     * Publishes the current issue now, rather than when the schedule says, if anyone's been active
     * since the last one. The next issue then covers from now.
     */
    public static Optional<Issue> publishNow(MinecraftServer server, DigestConfig config, Clock clock) {
        return publishNow(server, config, ISSUE, clock);
    }

    /**
     * Publishes the current issue if the schedule has come round since it started (see
     * {@link StatPeriods#check}).
     */
    static StatPeriods.Outcome check(MinecraftServer server, DigestConfig config, String period, Clock clock) {
        return PERIODS.check(server, period, config.schedule(), clock, ended -> publish(server, config, period, ended));
    }

    static Optional<Issue> publishNow(MinecraftServer server, DigestConfig config, String period, Clock clock) {
        final var published = new AtomicReference<Issue>();
        PERIODS.endNow(server, period, config.schedule(), clock, ended ->
            publish(server, config, period, ended).ifPresent(published::set));
        return Optional.ofNullable(published.get());
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
            Moments.take(server)
        );

        issue.ifPresent(found -> {
            Archive.add(server, found);
            DigestEvents.PUBLISHED.invoker().onPublished(server, found);
        });
        return issue;
    }
}
