package dev.seeruk.bundle.core.testing;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Test utilities for scoreboard scores, e.g. reading a player's score for a {@link ScoreboardStat},
 * so stats tests can compare scores before and after an action.
 */
public final class StatScores {
    private StatScores() {}

    /**
     * Returns the player's current score for the given stat, or 0 if they don't have one yet.
     * Throws if the stat's objective doesn't exist, so tests fail with a clear message.
     */
    public static int score(Player player, ScoreboardStat stat) {
        final var scoreboard = player.level().getServer().getScoreboard();
        final var objective = scoreboard.getObjective(stat.objectiveId());
        if (objective == null) {
            throw new IllegalStateException("Objective " + stat.objectiveId() + " doesn't exist");
        }

        final var info = scoreboard.getPlayerScoreInfo(player, objective);
        return info == null ? 0 : info.value();
    }

    /**
     * Adds a brand-new objective, removing any existing one with the same name first. The
     * scoreboard is shared by every test on the server, so each test should use its own name.
     */
    public static Objective addObjective(MinecraftServer server, String name) {
        final var scoreboard = server.getScoreboard();
        final var existing = scoreboard.getObjective(name);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }

        return scoreboard.addObjective(
            name, ObjectiveCriteria.DUMMY, Component.literal(name), ObjectiveCriteria.RenderType.INTEGER, false, null
        );
    }

    /**
     * Sets the player's score for the objective.
     */
    public static void setScore(ServerPlayer player, Objective objective, int score) {
        player.level().getServer().getScoreboard().getOrCreatePlayerScore(player, objective).set(score);
    }
}
