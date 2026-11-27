package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.api.Issue.PlayerTotal;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.StatScores.setScore;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestTimes.at;
import static dev.seeruk.bundle.core.testing.TestTimes.uniqueDay;
import static dev.seeruk.bundle.digest.DigestFixtures.config;
import static dev.seeruk.bundle.digest.DigestFixtures.publishedAs;

public class IssueScheduleGameTest {
    // Issues are published every day at 18:00 (see TestTimes.DAILY), and each check is given a
    // clock fixed at a time (see TestTimes.at), so these don't depend on the real clock. Each test
    // uses its own period and day. The rules for when periods start and end are tested in core's
    // StatPeriodsGameTest.

    // That day's 18:00, when an issue is published
    private static Instant sixPm(ZonedDateTime day) {
        return day.withHour(18).toInstant();
    }

    // The player's total in the issue's test group, if they scored
    private static Optional<Long> total(Issue issue, ServerPlayer player) {
        return issue.groups().get("test").players().stream()
            .filter(total -> total.player().equals(player.getUUID()))
            .map(PlayerTotal::value)
            .findFirst();
    }

    // Everyone who died in the issue, in order
    private static List<UUID> diedIn(Issue issue) {
        return issue.moments().deaths().stream().map(Issue.Death::player).toList();
    }

    @GameTest
    public void scheduleComingRoundPublishesAndArchivesAnIssue(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_scheduled");
        final var config = config(objective);
        final var player = player(helper);

        try {
            IssueSchedule.check(server, config, "scheduled", at(day));
            setScore(player, objective, 5);

            // The next day's 18:00 has passed, so the issue is published
            IssueSchedule.check(server, config, "scheduled", at(day.plusDays(1)));

            final var issue = Archive.latest(server).orElseThrow();
            // The first issue started counting when the schedule was first checked
            helper.assertValueEqual(issue.started(), day.toInstant(), "started");
            helper.assertValueEqual(issue.ended(), sixPm(day.plusDays(1)), "ended");
            helper.assertValueEqual(total(issue, player), Optional.of(5L), "total");
            helper.assertValueEqual(publishedAs(issue.number()), List.of(issue), "published");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void publishNowPublishesStraightAway(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_publish_now");
        final var config = config(objective);
        final var player = player(helper);

        try {
            IssueSchedule.check(server, config, "publish_now", at(day));
            setScore(player, objective, 3);

            final var issue = IssueSchedule.publishNow(
                server,
                config,
                "publish_now",
                at(day.plusHours(1))
            ).orElseThrow();

            helper.assertValueEqual(issue.ended(), day.plusHours(1).toInstant(), "ended");
            helper.assertValueEqual(total(issue, player), Optional.of(3L), "total");
            helper.assertValueEqual(Archive.get(server, issue.number()), Optional.of(issue), "archived");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void nothingIsPublishedWhenNobodyWasActive(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_empty");
        final var config = config(objective);

        try {
            // Starting the issue forgets any moments still pending from elsewhere, and nothing can
            // change between these, as they're in the same tick
            IssueSchedule.check(server, config, "empty", at(day));
            final var next = Archive.nextIssueNumber(server);
            final var issue = IssueSchedule.publishNow(server, config, "empty", at(day.plusHours(1)));

            helper.assertValueEqual(issue, Optional.empty(), "issue");
            helper.assertValueEqual(Archive.nextIssueNumber(server), next, "next number");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void issueNumbersCarryOn(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_numbers");
        final var config = config(objective);
        final var player = player(helper);

        try {
            IssueSchedule.check(server, config, "numbers", at(day));
            setScore(player, objective, 1);
            final var first = IssueSchedule.publishNow(server, config, "numbers", at(day.plusHours(1))).orElseThrow();
            setScore(player, objective, 2);
            final var second = IssueSchedule.publishNow(server, config, "numbers", at(day.plusHours(2))).orElseThrow();

            helper.assertValueEqual(second.number(), first.number() + 1, "second number");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void newPlayersAreThoseWithNoStatsWhenTheIssueStarted(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_new");
        final var config = config(objective);
        final var existing = player(helper);

        try {
            IssueSchedule.check(server, config, "new", at(day));
            final var joined = player(helper);
            setScore(existing, objective, 1);
            setScore(joined, objective, 1);

            final var issue = IssueSchedule.publishNow(server, config, "new", at(day.plusHours(1))).orElseThrow();

            helper.assertTrue(issue.activePlayers().contains(existing.getUUID()), "Existing player should be active");
            helper.assertTrue(issue.newPlayers().contains(joined.getUUID()), "Player who joined should be new");
            helper.assertTrue(!issue.newPlayers().contains(existing.getUUID()), "Existing player shouldn't be new");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void publishedIssuesHaveTheMomentsSinceTheLast(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_moments");
        final var config = config(objective);
        final var before = player(helper);
        final var after = player(helper);

        try {
            IssueSchedule.check(server, config, "moments", at(day));
            Moments.died(before, helper.getLevel().damageSources().fall(), at(day.plusMinutes(30)));
            final var first = IssueSchedule.publishNow(server, config, "moments", at(day.plusHours(1))).orElseThrow();
            Moments.died(after, helper.getLevel().damageSources().fall(), at(day.plusMinutes(90)));
            final var second = IssueSchedule.publishNow(server, config, "moments", at(day.plusHours(2))).orElseThrow();

            helper.assertValueEqual(diedIn(first), List.of(before.getUUID()), "died in the first issue");
            helper.assertValueEqual(diedIn(second), List.of(after.getUUID()), "died in the second issue");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void startingAnIssueWithoutPublishingForgetsPendingMoments(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_forget");
        final var config = config(objective);
        final var before = player(helper);
        final var after = player(helper);

        try {
            // Died before the issue started, e.g. before the schedule was changed
            Moments.died(before, helper.getLevel().damageSources().fall(), at(day.minusMinutes(30)));
            IssueSchedule.check(server, config, "forget", at(day));
            Moments.died(after, helper.getLevel().damageSources().fall(), at(day.plusMinutes(30)));

            final var issue = IssueSchedule.publishNow(server, config, "forget", at(day.plusHours(1))).orElseThrow();

            helper.assertValueEqual(diedIn(issue), List.of(after.getUUID()), "died in the issue");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
