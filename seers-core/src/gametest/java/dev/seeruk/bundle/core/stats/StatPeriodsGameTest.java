package dev.seeruk.bundle.core.stats;

import dev.seeruk.bundle.core.stats.StatPeriods.Ended;
import dev.seeruk.bundle.core.stats.StatPeriods.Outcome;
import dev.seeruk.bundle.core.time.Schedule;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.StatScores.setScore;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestTimes.DAILY;
import static dev.seeruk.bundle.core.testing.TestTimes.at;
import static dev.seeruk.bundle.core.testing.TestTimes.uniqueDay;

public class StatPeriodsGameTest {
    // Periods end every day at 18:00 (see TestTimes.DAILY), and each check is given a clock fixed
    // at a time (see TestTimes.at), so these don't depend on the real clock. Each test uses its own
    // period and day.

    private static final StatPeriods PERIODS = StatPeriods.of("stat-periods-test");

    // The given day's 18:00, when our test period ends
    private static Instant sixPm(ZonedDateTime day) {
        return day.withHour(18).toInstant();
    }

    @GameTest
    public void firstCheckStartsQuietly(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var endings = new ArrayList<Ended>();

        final var outcome = PERIODS.check(server, "first", DAILY, at(uniqueDay()), endings::add);

        helper.assertValueEqual(outcome, Outcome.STARTED, "outcome");
        helper.assertValueEqual(endings, List.of(), "endings");
        helper.succeed();
    }

    @GameTest
    public void sinceStartOnlyCountsWhatHappensAfterTheStart(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "stat_periods_since");
        final var player = player(helper);

        try {
            // The first check starts the period, snapshotting the 5 the player already has
            setScore(player, objective, 5);
            PERIODS.check(server, "since", DAILY, at(uniqueDay()), ended -> {});

            // So only the 3 gained since counts
            setScore(player, objective, 8);
            final var since = PERIODS.sinceStart(server, "since", AllPlayerStats.read(server));

            helper.assertValueEqual(
                Optional.ofNullable(since.getOrDefault(player.getUUID(), Map.of()).get("scoreboard:stat_periods_since")),
                Optional.of(3),
                "since start"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void sinceStartIsEmptyBeforeAPeriodStarts(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();

        helper.assertValueEqual(PERIODS.sinceStart(server, "never_started", AllPlayerStats.read(server)), Map.of(), "since start");
        helper.succeed();
    }

    @GameTest
    public void periodEndsOnceAtItsSchedule(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var endings = new ArrayList<Ended>();
        PERIODS.check(server, "ends", DAILY, at(day), endings::add);

        // The next day's 18:00 has passed, so it ends. Checking again does nothing, as the next
        // period started at that 18:00.
        final var first = PERIODS.check(server, "ends", DAILY, at(day.plusDays(1)), endings::add);
        final var second = PERIODS.check(server, "ends", DAILY, at(day.plusDays(1)), endings::add);

        helper.assertValueEqual(List.of(first, second), List.of(Outcome.ENDED, Outcome.UNCHANGED), "outcomes");
        // The first period started when it was first checked, not at the 18:00 before that
        helper.assertValueEqual(endings, List.of(new Ended(day.toInstant(), sixPm(day.plusDays(1)), false)), "endings");
        helper.succeed();
    }

    @GameTest
    public void periodDoesNotEndBeforeItsSchedule(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        PERIODS.check(server, "early", DAILY, at(day), ended -> {});

        // Still before the next day's 18:00
        final var outcome = PERIODS.check(server, "early", DAILY, at(day.plusHours(4)), ended -> {});

        helper.assertValueEqual(outcome, Outcome.UNCHANGED, "outcome");
        helper.succeed();
    }

    @GameTest
    public void periodEndsOnceAfterSeveralSchedulesPass(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var endings = new ArrayList<Ended>();
        PERIODS.check(server, "several", DAILY, at(day), endings::add);

        // Two 18:00s have passed, e.g. while the server was stopped, but it only ends once, and
        // says a whole period was missed
        PERIODS.check(server, "several", DAILY, at(day.plusDays(2)), endings::add);

        helper.assertValueEqual(endings, List.of(new Ended(day.toInstant(), sixPm(day.plusDays(2)), true)), "endings");
        helper.succeed();
    }

    @GameTest
    public void clockGoingBackBeforeTheStartStartsAgain(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var endings = new ArrayList<Ended>();
        PERIODS.check(server, "back", DAILY, at(day), endings::add);

        // The period started after this, so its start can't be trusted
        final var outcome = PERIODS.check(server, "back", DAILY, at(day.minusDays(1)), endings::add);

        helper.assertValueEqual(outcome, Outcome.STARTED, "outcome");
        helper.assertValueEqual(endings, List.of(), "endings");
        helper.succeed();
    }

    @GameTest
    public void periodStillEndsAfterTheClockJumpsForwardAndBack(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();

        // The clock jumps 5 years ahead, ending the period, and starting one in 5 years. When the
        // clock comes back, that one starts again from now, so it can still end at the next 18:00.
        final var outcomes = List.of(
            PERIODS.check(server, "jump", DAILY, at(day), ended -> {}),
            PERIODS.check(server, "jump", DAILY, at(day.plusYears(5)), ended -> {}),
            PERIODS.check(server, "jump", DAILY, at(day.plusHours(1)), ended -> {}),
            PERIODS.check(server, "jump", DAILY, at(day.plusDays(1)), ended -> {})
        );

        helper.assertValueEqual(
            outcomes,
            List.of(Outcome.STARTED, Outcome.ENDED, Outcome.STARTED, Outcome.ENDED),
            "outcomes"
        );
        helper.succeed();
    }

    @GameTest
    public void changedScheduleStartsAgain(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var endings = new ArrayList<Ended>();
        PERIODS.check(server, "changed", DAILY, at(day), endings::add);

        // 20:00 has passed under the new schedule, but the period never ran on it, so it starts
        // again, rather than ending
        final var changed = Schedule.daily(LocalTime.of(20, 0));
        final var outcome = PERIODS.check(server, "changed", changed, at(day.plusHours(2)), endings::add);

        helper.assertValueEqual(outcome, Outcome.STARTED, "outcome");
        helper.assertValueEqual(endings, List.of(), "endings");
        helper.succeed();
    }

    @GameTest
    public void endNowEndsThePeriodAndTheNextEndsOnSchedule(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var endings = new ArrayList<Ended>();
        PERIODS.check(server, "end_now", DAILY, at(day), endings::add);

        // Ends at 20:00, before the schedule says, so the next period starts then, and ends at
        // the next day's 18:00 as normal
        final var ended = PERIODS.endNow(server, "end_now", DAILY, at(day.plusHours(1)), endings::add);
        final var early = PERIODS.check(server, "end_now", DAILY, at(day.plusHours(2)), endings::add);
        final var onTime = PERIODS.check(server, "end_now", DAILY, at(day.plusDays(1)), endings::add);

        helper.assertValueEqual(List.of(ended, early, onTime), List.of(Outcome.ENDED, Outcome.UNCHANGED, Outcome.ENDED), "outcomes");
        helper.assertValueEqual(
            endings,
            List.of(
                new Ended(day.toInstant(), day.plusHours(1).toInstant(), false),
                new Ended(day.plusHours(1).toInstant(), sixPm(day.plusDays(1)), false)
            ),
            "endings"
        );
        helper.succeed();
    }

    @GameTest
    public void endNowStartsAPeriodThatHasntStarted(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var endings = new ArrayList<Ended>();

        final var outcome = PERIODS.endNow(server, "end_now_first", DAILY, at(uniqueDay()), endings::add);

        helper.assertValueEqual(outcome, Outcome.STARTED, "outcome");
        helper.assertValueEqual(endings, List.of(), "endings");
        helper.succeed();
    }

    @GameTest
    public void playersAtStartAreThoseWithStatsWhenItStarted(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var before = player(helper);
        PERIODS.check(server, "at_start", DAILY, at(uniqueDay()), ended -> {});
        final var after = player(helper);

        final var atStart = PERIODS.playersAtStart(server, "at_start");

        helper.assertTrue(atStart.contains(before.getUUID()), "Player from before the start should be included");
        helper.assertTrue(!atStart.contains(after.getUUID()), "Player from after the start shouldn't be included");
        helper.succeed();
    }

    @GameTest
    public void forgetExceptForgetsOnlyTheModsOtherPeriods(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        // A mod of its own, so other tests' periods aren't forgotten
        final var periods = StatPeriods.of("stat-periods-forget-test");
        final var other = StatPeriods.of("stat-periods-forget-other-test");
        final var day = uniqueDay();
        periods.check(server, "kept", DAILY, at(day), ended -> {});
        periods.check(server, "removed", DAILY, at(day), ended -> {});
        other.check(server, "removed", DAILY, at(day), ended -> {});

        final var forgotten = periods.forgetExcept(server, List.of("kept"));

        helper.assertValueEqual(forgotten, List.of("removed"), "forgotten");
        // A forgotten period starts again, but the other mod's period of the same name doesn't
        helper.assertValueEqual(periods.check(server, "removed", DAILY, at(day), ended -> {}), Outcome.STARTED, "removed");
        helper.assertValueEqual(periods.check(server, "kept", DAILY, at(day), ended -> {}), Outcome.UNCHANGED, "kept");
        helper.assertValueEqual(other.check(server, "removed", DAILY, at(day), ended -> {}), Outcome.UNCHANGED, "other mod's");
        helper.succeed();
    }
}
