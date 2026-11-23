package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.MOD_ID;

/**
 * Keeps per-player resource zone statistics in Vanilla scoreboard objectives (see
 * {@link ScoreboardStat}).
 */
public final class ResourceZonesStats {
    public static final String ZONE_TICKS_OBJECTIVE = "zone_ticks";
    public static final String ZONE_ENTRIES_OBJECTIVE = "zone_entries";
    public static final String BLOCKS_MINED_OBJECTIVE = "blocks_mined";

    /**
     * Total number of ticks each player has spent in resource zones.
     */
    static final ScoreboardStat ZONE_TICKS = new ScoreboardStat(MOD_ID, ZONE_TICKS_OBJECTIVE, "Resource Zone Ticks");

    /**
     * Total number of times each player has entered a resource zone.
     */
    static final ScoreboardStat ZONE_ENTRIES = new ScoreboardStat(
        MOD_ID,
        ZONE_ENTRIES_OBJECTIVE,
        "Resource Zone Entries"
    );

    /**
     * Total number of blocks each player has broken in resource zones.
     */
    static final ScoreboardStat BLOCKS_MINED = new ScoreboardStat(
        MOD_ID,
        BLOCKS_MINED_OBJECTIVE,
        "Resource Zone Blocks Mined"
    );

    private ResourceZonesStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        ZONE_TICKS.ensureObjective(server);
        ZONE_ENTRIES.ensureObjective(server);
        BLOCKS_MINED.ensureObjective(server);
    }

    /**
     * Adds one to the score of every player who's in a resource zone.
     */
    public static void recordZoneTick(MinecraftServer server) {
        for (final var player : server.getPlayerList().getPlayers()) {
            if (ZonePresence.isInZone(player)) {
                ZONE_TICKS.add(player, 1);
            }
        }
    }

    public static void recordZoneEntered(ServerPlayer player) {
        ZONE_ENTRIES.add(player, 1);
    }

    /**
     * Adds one to the player's score if the block they broke was in a resource zone.
     */
    public static void recordBlockMined(Level level, Player player, BlockPos pos) {
        if (ResourceZones.contains(level.getServer(), level.dimension(), ChunkPos.containing(pos))) {
            BLOCKS_MINED.add(player, 1);
        }
    }
}
