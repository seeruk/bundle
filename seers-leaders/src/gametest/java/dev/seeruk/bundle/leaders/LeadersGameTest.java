package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.leaders.Leaderboard.Standing;
import dev.seeruk.bundle.leaders.LeadersFixtures.Changed;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestPlayers.systemMessages;
import static dev.seeruk.bundle.leaders.LeadersFixtures.addObjective;
import static dev.seeruk.bundle.leaders.LeadersFixtures.category;
import static dev.seeruk.bundle.leaders.LeadersFixtures.config;
import static dev.seeruk.bundle.leaders.LeadersFixtures.eventsFor;

public class LeadersGameTest {
    private static void setScore(ServerPlayer player, Objective objective, int score) {
        player.level().getServer().getScoreboard().getOrCreatePlayerScore(player, objective).set(score);
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    // Announcements go to everyone, including other tests' players, so only those naming the given
    // player are kept
    private static List<String> announcementsFor(ServerPlayer player) {
        return systemMessages(player).stream().filter(message -> message.startsWith(name(player))).toList();
    }

    @GameTest
    public void refreshRanksPlayersByTheirTotals(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_ranks");
        final var config = config("ranks", objective);
        final var first = player(helper);
        final var second = player(helper);

        try {
            setScore(second, objective, 3);
            setScore(first, objective, 5);
            Leaders.refresh(server, config);

            helper.assertValueEqual(Leaders.standing(new Seat("ranks", 1)), Optional.of(new Standing(first.getUUID(), 5)), "first");
            helper.assertValueEqual(Leaders.standing(new Seat("ranks", 2)), Optional.of(new Standing(second.getUUID(), 3)), "second");
            helper.assertValueEqual(Leaders.standing(new Seat("ranks", 3)), Optional.empty(), "third");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void firstRefreshDoesNotFireEvent(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_first_refresh");
        final var player = player(helper);

        try {
            setScore(player, objective, 1);
            Leaders.refresh(server, config("first_refresh", objective));

            helper.assertValueEqual(eventsFor("first_refresh"), List.of(), "events");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void newLeaderFiresEventOnce(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_new_leader");
        final var config = config("new_leader", objective);
        final var leader = player(helper);
        final var overtaker = player(helper);

        try {
            setScore(leader, objective, 5);
            setScore(overtaker, objective, 3);
            Leaders.refresh(server, config);

            setScore(overtaker, objective, 6);
            Leaders.refresh(server, config);
            Leaders.refresh(server, config);

            helper.assertValueEqual(
                eventsFor("new_leader"),
                List.of(new Changed("new_leader", Optional.of(leader.getUUID()), Optional.of(overtaker.getUUID()))),
                "events"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void tieDoesNotChangeLeader(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_tie");
        final var config = config("tie", objective);
        final var leader = player(helper);
        final var challenger = player(helper);

        try {
            setScore(leader, objective, 5);
            setScore(challenger, objective, 3);
            Leaders.refresh(server, config);

            setScore(challenger, objective, 5);
            Leaders.refresh(server, config);

            helper.assertValueEqual(Leaders.standing(new Seat("tie", 1)).map(Standing::player), Optional.of(leader.getUUID()), "leader");
            helper.assertValueEqual(eventsFor("tie"), List.of(), "events");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void newLeaderIsAnnounced(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_announced");
        final var config = config("announced", objective);
        final var leader = player(helper);
        final var overtaker = player(helper);

        try {
            setScore(leader, objective, 5);
            Leaders.refresh(server, config);

            setScore(overtaker, objective, 6);
            Leaders.refresh(server, config);

            helper.assertValueEqual(announcementsFor(overtaker), List.of(name(overtaker) + " leads Test"), "announcements");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void firstLeaderIsNotAnnounced(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_first_leader");
        final var config = config("first_leader", objective);
        final var player = player(helper);

        try {
            // Nobody has scored, so nobody is recorded as the leader
            Leaders.refresh(server, config);

            // Taking over from nobody is a change, but being first in an empty category isn't news
            setScore(player, objective, 1);
            Leaders.refresh(server, config);

            helper.assertValueEqual(eventsFor("first_leader").size(), 1, "events");
            helper.assertValueEqual(announcementsFor(player), List.of(), "announcements");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void categoryThatDoesNotAnnounceChangesIsQuiet(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "leaders_quiet");
        final var config = config(category("quiet", objective, false));
        final var leader = player(helper);
        final var overtaker = player(helper);

        try {
            setScore(leader, objective, 5);
            Leaders.refresh(server, config);

            setScore(overtaker, objective, 6);
            Leaders.refresh(server, config);

            helper.assertValueEqual(eventsFor("quiet").size(), 1, "events");
            helper.assertValueEqual(announcementsFor(overtaker), List.of(), "announcements");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void newLeadersAtTheSameTimeAreAnnouncedTogether(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var first = addObjective(server, "leaders_together_first");
        final var second = addObjective(server, "leaders_together_second");
        final var config = config(category("together_first", first, true), category("together_second", second, true));
        final var leader = player(helper);
        final var overtaker = player(helper);

        try {
            setScore(leader, first, 5);
            setScore(leader, second, 5);
            Leaders.refresh(server, config);

            // Overtakes in both categories before the next refresh
            setScore(overtaker, first, 6);
            setScore(overtaker, second, 6);
            Leaders.refresh(server, config);

            helper.assertValueEqual(
                announcementsFor(overtaker),
                List.of(name(overtaker) + " leads Test\n" + name(overtaker) + " leads Test"),
                "announcements"
            );
        } finally {
            server.getScoreboard().removeObjective(first);
            server.getScoreboard().removeObjective(second);
        }

        helper.succeed();
    }
}
