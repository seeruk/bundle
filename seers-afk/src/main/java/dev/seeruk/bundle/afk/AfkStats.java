package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.server.MinecraftServer;

import static dev.seeruk.bundle.afk.SeersAFK.MOD_ID;

/**
 * Keeps per-player AFK statistics in Vanilla scoreboard objectives (see {@link ScoreboardStat}).
 */
public final class AfkStats {
    public static final String AFK_TICKS_OBJECTIVE = "afk_ticks";

    /**
     * Total number of ticks each player has been AFK for.
     */
    static final ScoreboardStat AFK_TICKS = new ScoreboardStat(MOD_ID, AFK_TICKS_OBJECTIVE, "AFK Ticks");

    private AfkStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        AFK_TICKS.ensureObjective(server);
    }

    /**
     * Adds one to the score of every player who's AFK.
     */
    public static void recordAfkTick(MinecraftServer server) {
        for (final var player : server.getPlayerList().getPlayers()) {
            if (AfkStatus.isAfk(player)) {
                AFK_TICKS.add(player, 1);
            }
        }
    }
}
