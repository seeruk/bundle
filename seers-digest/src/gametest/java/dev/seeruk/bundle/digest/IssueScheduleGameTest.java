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

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.StatScores.setScore;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestTimes.uniqueDay;
import static dev.seeruk.bundle.digest.DigestFixtures.config;
import static dev.seeruk.bundle.digest.DigestFixtures.publishedAs;

public class IssueScheduleGameTest {
    // Issues are published every day at 18:00 (see TestTimes.DAILY), and each check says what the
    // time is, so these don't depend on the real clock. Each test uses its own period and day. The
    // rules for when periods start and end are tested in core's StatPeriodsGameTest.

    // The moment that day's issue was published, and the next started
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

    @GameTest
    public void scheduleComingRoundPublishesAndArchivesAnIssue(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var day = uniqueDay();
        final var objective = addObjective(server, "issue_schedule_scheduled");
        final var config = config(objective);
        final var player = player(helper);

        try {
            IssueSchedule.check(server, config, "scheduled", day);
            setScore(player, objective, 5);

            // The next day's 18:00 has passed, so the issue is published
            IssueSchedule.check(server, config, "scheduled", day.plusDays(1));

            final var issue = Archive.latest(server).orElseThrow();
            helper.assertValueEqual(issue.started(), sixPm(day), "started");
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
            IssueSchedule.check(server, config, "publish_now", day);
            setScore(player, objective, 3);

            final var issue = IssueSchedule.publishNow(server, config, "publish_now", day.plusHours(1)).orElseThrow();

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
            // Nothing can change between these, as they're in the same tick
            IssueSchedule.check(server, config, "empty", day);
            final var next = Archive.nextIssueNumber(server);
            final var issue = IssueSchedule.publishNow(server, config, "empty", day.plusHours(1));

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
            IssueSchedule.check(server, config, "numbers", day);
            setScore(player, objective, 1);
            final var first = IssueSchedule.publishNow(server, config, "numbers", day.plusHours(1)).orElseThrow();
            setScore(player, objective, 2);
            final var second = IssueSchedule.publishNow(server, config, "numbers", day.plusHours(2)).orElseThrow();

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
            IssueSchedule.check(server, config, "new", day);
            final var joined = player(helper);
            setScore(existing, objective, 1);
            setScore(joined, objective, 1);

            final var issue = IssueSchedule.publishNow(server, config, "new", day.plusHours(1)).orElseThrow();

            helper.assertTrue(issue.activePlayers().contains(existing.getUUID()), "Existing player should be active");
            helper.assertTrue(issue.newPlayers().contains(joined.getUUID()), "Player who joined should be new");
            helper.assertTrue(!issue.newPlayers().contains(existing.getUUID()), "Existing player shouldn't be new");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
