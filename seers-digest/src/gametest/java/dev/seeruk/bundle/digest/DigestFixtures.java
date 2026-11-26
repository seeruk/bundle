package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.digest.DigestConfig.StatGroup;
import dev.seeruk.bundle.digest.api.DigestEvents;
import dev.seeruk.bundle.digest.api.Issue;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.scores.Objective;

import java.time.ZoneOffset;
import java.util.List;

import static dev.seeruk.bundle.core.testing.TestTimes.DAILY;

/**
 * Shared helpers for Digest's GameTests. Starts recording published issues when the game starts,
 * so every test's issues are recorded.
 *
 * <p>The scoreboard, the archive, and every player are shared by every test on the server, so each
 * test uses its own period and objective, and only checks its own players.
 */
public final class DigestFixtures implements ModInitializer {
    private static final EventLog<Issue> PUBLISHED = new EventLog<>();

    @Override
    public void onInitialize() {
        DigestEvents.PUBLISHED.register((server, issue) -> PUBLISHED.record(issue));
    }

    /**
     * Every issue published with the given number.
     */
    public static List<Issue> publishedAs(int number) {
        return PUBLISHED.matching(issue -> issue.number() == number);
    }

    /**
     * A config whose issues are published every day at 18:00 (see {@code TestTimes.DAILY}), with
     * one group, called {@code test}, adding up the given objective's scores.
     */
    public static DigestConfig config(Objective objective) {
        return new DigestConfig(
            ZoneOffset.UTC,
            DAILY,
            List.of(new StatGroup("test", StatSelection.of(List.of("scoreboard:" + objective.getName()))))
        );
    }
}
