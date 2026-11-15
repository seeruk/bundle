package dev.seeruk.bundle.propswap;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import static dev.seeruk.bundle.propswap.SeersPropSwap.MOD_ID;

/**
 * Keeps per-player PropSwap statistics in Vanilla scoreboard objectives (see
 * {@link ScoreboardStat}).
 */
public final class PropSwapStats {
    public static final String SWAPPED_OBJECTIVE = "blocks_swapped";

    /**
     * Total number of blocks each player has PropSwapped.
     */
    static final ScoreboardStat SWAPPED = new ScoreboardStat(MOD_ID, SWAPPED_OBJECTIVE, "Blocks PropSwapped");

    private PropSwapStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        SWAPPED.ensureObjective(server);
    }

    /**
     * Adds one to the player's score.
     */
    public static void recordSwapped(Player player, Level level, BlockPos pos, BlockState original, BlockState swapped) {
        SWAPPED.add(player, 1);
    }
}
