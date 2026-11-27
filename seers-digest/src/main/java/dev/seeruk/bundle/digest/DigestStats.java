package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.time.Clock;
import java.time.LocalTime;

import static dev.seeruk.bundle.digest.SeersDigest.MOD_ID;

/**
 * Keeps per-player statistics Vanilla doesn't, in scoreboard objectives (see
 * {@link ScoreboardStat}), so the digest, and anything else, can use them as stats.
 */
public final class DigestStats {
    /**
     * Total number of ticks each player has played during the night (see {@link NightHours}).
     */
    static final ScoreboardStat NIGHT_TICKS = new ScoreboardStat(MOD_ID, "night_ticks", "Night Owl Ticks");

    /**
     * Total number of times each player has gone to the Nether.
     */
    static final ScoreboardStat NETHER_VISITS = new ScoreboardStat(MOD_ID, "nether_visits", "Nether Visits");

    /**
     * Total number of times each player has gone to the End.
     */
    static final ScoreboardStat END_VISITS = new ScoreboardStat(MOD_ID, "end_visits", "End Visits");

    private DigestStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        NIGHT_TICKS.ensureObjective(server);
        NETHER_VISITS.ensureObjective(server);
        END_VISITS.ensureObjective(server);
    }

    /**
     * Adds a second's worth of ticks to every player playing during the night, so it should be
     * called once a second.
     */
    public static void recordNightSecond(MinecraftServer server, NightHours night, Clock clock) {
        if (!night.contains(LocalTime.now(clock))) {
            return;
        }

        for (final var player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator()) {
                NIGHT_TICKS.add(player, SharedConstants.TICKS_PER_SECOND);
            }
        }
    }

    /**
     * Counts a visit to the Nether or the End, if that's where the player's gone.
     */
    public static void recordDimensionChange(ServerPlayer player, ResourceKey<Level> destination) {
        if (destination == Level.NETHER) {
            NETHER_VISITS.add(player, 1);
        } else if (destination == Level.END) {
            END_VISITS.add(player, 1);
        }
    }
}
