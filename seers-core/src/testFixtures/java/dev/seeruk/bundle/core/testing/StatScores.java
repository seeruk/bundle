package dev.seeruk.bundle.core.testing;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.world.entity.player.Player;

/**
 * A utility for reading a player's score for a {@link ScoreboardStat} in tests, so stats tests can
 * compare scores before and after an action.
 */
public final class StatScores {
    private StatScores() {}

    /**
     * Returns the player's current score for the given stat, or 0 if they don't have one yet. Throws
     * if the stat's objective doesn't exist, so tests fail with a clear message.
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
}
