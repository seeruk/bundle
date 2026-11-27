package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.leaders.Leaderboard.Standing;
import dev.seeruk.bundle.leaders.LeadersFixtures.PeriodEnded;
import dev.seeruk.bundle.leaders.LeadersFixtures.Won;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.StatScores.setScore;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestPlayers.systemMessages;
import static dev.seeruk.bundle.core.testing.TestTimes.DAILY;
import static dev.seeruk.bundle.core.testing.TestTimes.at;
import static dev.seeruk.bundle.core.testing.TestTimes.uniqueDay;

public class PeriodsGameTest {
    // Periods end every day at 18:00 (see TestTimes.DAILY), and each check is given a clock fixed
    // at a time (see TestTimes.at), so these don't depend on the real clock. Each test starts on
    // its own day. The rules for when periods start and end are tested in core's
    // StatPeriodsGameTest, so these cover what Leaders adds on top.

    @GameTest
    public void periodOnlyCountsWhatHappensAfterItStarts(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "periods_counts");
        final var config = periodConfig("counts", objective, DAILY);
        final var player = player(helper);

        try {
            // The first check starts the period, snapshotting the 5 the player already has
            setScore(player, objective, 5);
            Periods.check(server, config, at(day));

            // So only the 3 gained since counts
            setScore(player, objective, 8);
            Leaders.refresh(server, config);

            helper.assertValueEqual(
                Leaders.standing(new Seat("counts", 1)),
                Optional.of(new Standing(player.getUUID(), 3)),
                "standing"
            );
            helper.assertValueEqual(periodEventsFor("counts"), List.of(), "events");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void endingPeriodFiresEventOnceSavesResultsAndStartsAgain(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "periods_ends");
        final var config = periodConfig("ends", objective, DAILY);
        final var player = player(helper);

        try {
            Periods.check(server, config, at(day));
            setScore(player, objective, 4);

            // The next day's 18:00 has passed, so the period ends. Checking again does nothing, as
            // the new period started at that 18:00.
            Periods.check(server, config, at(day.plusDays(1)));
            Periods.check(server, config, at(day.plusDays(1)));

            // The 4 is saved as last period's result, and the new period counts from 4, so nobody
            // has scored in it yet

            // Here, we're expecting to get the event for the period that initially ended when we
            // checked the day after the period started.
            helper.assertValueEqual(
                periodEventsFor("ends"),
                List.of(new PeriodEnded("ends", List.of(new Won("ends", player.getUUID(), 4, "4")), true)),
                "events"
            );

            // And then we check the previous and current standings
            helper.assertValueEqual(
                Leaders.previous(server, new Seat("ends", 1)),
                Optional.of(new Standing(player.getUUID(), 4)),
                "previous"
            );
            helper.assertValueEqual(Leaders.standing(new Seat("ends", 1)), Optional.empty(), "standing once ended");

            // The new period counts what's gained after it started
            setScore(player, objective, 6);
            Leaders.refresh(server, config);
            helper.assertValueEqual(
                Leaders.standing(new Seat("ends", 1)),
                Optional.of(new Standing(player.getUUID(), 2)),
                "standing in the new period"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void periodNobodyScoredInHasNoWinners(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "periods_nobody");
        final var config = periodConfig("nobody", objective, DAILY);
        final var player = player(helper);

        try {
            // The first period has a winner
            Periods.check(server, config, at(day));
            setScore(player, objective, 4);
            Periods.check(server, config, at(day.plusDays(1)));

            // The second doesn't, as the score didn't change after it started
            Periods.check(server, config, at(day.plusDays(2)));

            helper.assertValueEqual(
                periodEventsFor("nobody").getLast(),
                new PeriodEnded("nobody", List.of(), false),
                "last event"
            );
            helper.assertValueEqual(Leaders.previous(server, new Seat("nobody", 1)), Optional.empty(), "previous");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void endingPeriodIsAnnounced(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "periods_announced");
        final var config = periodConfig("announced", objective, DAILY);
        final var player = player(helper);

        try {
            Periods.check(server, config, at(day));
            setScore(player, objective, 4);

            Periods.check(server, config, at(day.plusDays(1)));

            // Announcements go to everyone, including other tests' players, so only this test's
            // period is looked for
            helper.assertValueEqual(
                systemMessages(player).stream().filter(message -> message.startsWith("announced ended")).toList(),
                List.of("announced ended\nTest: " + player.getGameProfile().name()),
                "announcements"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void periodEndingAfterWholePeriodsWereMissedIsNotAnnounced(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "periods_missed");
        final var config = periodConfig("missed", objective, DAILY);
        final var player = player(helper);

        try {
            Periods.check(server, config, at(day));
            setScore(player, objective, 4);

            // Two 18:00s have passed, so the next day's period was missed entirely. The period
            // still ends, but its winners could be from long ago, so they aren't announced.
            Periods.check(server, config, at(day.plusDays(2)));

            helper.assertValueEqual(
                periodEventsFor("missed"),
                List.of(new PeriodEnded("missed", List.of(new Won("missed", player.getUUID(), 4, "4")), false)),
                "events"
            );
            helper.assertValueEqual(
                systemMessages(player).stream().filter(message -> message.startsWith("missed ended")).toList(),
                List.of(),
                "announcements"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void changedScheduleStartsAgainQuietly(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "periods_changed");
        final var player = player(helper);

        try {
            Periods.check(server, periodConfig("changed", objective, DAILY), at(day));
            setScore(player, objective, 4);

            // 20:00 has passed under the new schedule, but the old period never ran on it, so it
            // starts again from now, rather than ending with winners
            final var changed = periodConfig("changed", objective, Schedule.daily(LocalTime.of(20, 0)));
            Periods.check(server, changed, at(day.plusHours(2)));

            // Had it not started again, the old snapshot would still count the 4
            Leaders.refresh(server, changed);

            helper.assertValueEqual(periodEventsFor("changed"), List.of(), "events");
            helper.assertValueEqual(Leaders.standing(new Seat("changed", 1)), Optional.empty(), "standing");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
