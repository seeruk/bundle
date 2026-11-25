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
import static dev.seeruk.bundle.leaders.LeadersFixtures.addObjective;
import static dev.seeruk.bundle.leaders.LeadersFixtures.config;
import static dev.seeruk.bundle.leaders.LeadersFixtures.eventsFor;

public class LeadersGameTest {
    private static void setScore(ServerPlayer player, Objective objective, int score) {
        player.level().getServer().getScoreboard().getOrCreatePlayerScore(player, objective).set(score);
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

            helper.assertValueEqual(Leaders.standing("ranks", 1), Optional.of(new Standing(first.getUUID(), 5)), "first");
            helper.assertValueEqual(Leaders.standing("ranks", 2), Optional.of(new Standing(second.getUUID(), 3)), "second");
            helper.assertValueEqual(Leaders.standing("ranks", 3), Optional.empty(), "third");
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

            helper.assertValueEqual(Leaders.standing("tie", 1).map(Standing::player), Optional.of(leader.getUUID()), "leader");
            helper.assertValueEqual(eventsFor("tie"), List.of(), "events");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
