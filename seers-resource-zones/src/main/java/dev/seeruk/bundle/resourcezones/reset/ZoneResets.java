package dev.seeruk.bundle.resourcezones.reset;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.core.storage.PlayerStorage;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.MOD_ID;

/**
 * A service for detecting resource zone resets (see {@link ResetHistory}), and moving players out
 * of chunks that were reset while they were away.
 *
 * <p>Zones are only reset while the server's stopped, so resets are found as the server starts,
 * before any chunks are loaded (which would regenerate them), by checking which generated chunks
 * are no longer stored in region files.
 */
public final class ZoneResets {
    private static final WorldStorage<ResetHistory> HISTORY = WorldStorage.create(
        MOD_ID,
        "zone_resets",
        ResetHistory.CODEC,
        () -> ResetHistory.EMPTY
    );

    // The reset count each player last saw, when they last joined
    private static final PlayerStorage<Integer> SEEN = PlayerStorage.create(MOD_ID, "resets_seen", Codec.INT, () -> 0);

    // Dimension -> its reset chunks, found as levels load, until the server's started. Concurrent,
    // as it's shared across levels.
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> FOUND = new ConcurrentHashMap<>();

    private ZoneResets() {}

    public static ResetHistory history(MinecraftServer server) {
        return HISTORY.get(server);
    }

    /**
     * Remembers the chunk as generated, if it's in a zone.
     */
    public static void rememberGenerated(ServerLevel level, LevelChunk chunk) {
        final var server = level.getServer();
        final var dimension = level.dimension();
        final var pos = chunk.getPos();
        if (ResourceZones.contains(server, dimension, pos) && !HISTORY.get(server).generated().contains(
            dimension,
            pos
        )) {
            HISTORY.update(server, history -> history.withGenerated(dimension, pos));
        }
    }

    /**
     * Brings the dimension's generated chunks up to date with its zones, adding zone chunks that
     * are loaded or stored, and dropping chunks that are no longer in a zone.
     */
    public static void refreshGenerated(MinecraftServer server, ResourceKey<Level> dimension) {
        final var level = server.getLevel(dimension);
        if (level == null) {
            return;
        }

        final var zone = ResourceZones.chunks(server, dimension);
        final var existing = new HashSet<>(RegionFiles.stored(RegionFiles.folder(server, dimension), zone));
        zone.stream().filter(chunk -> level.hasChunk(chunk.x(), chunk.z())).forEach(existing::add);

        HISTORY.update(server, history -> history.withRefreshedGenerated(dimension, zone, existing));
    }

    /**
     * Finds the level's reset chunks, holding them until the server's started.
     */
    public static void findResets(MinecraftServer server, ServerLevel level) {
        final var dimension = level.dimension();
        final var history = HISTORY.get(server);
        final var generated = history.generated().in(dimension);
        if (generated.isEmpty()) {
            return;
        }

        final var reset = history.missing(
            dimension,
            RegionFiles.stored(RegionFiles.folder(server, dimension), generated)
        );
        if (!reset.isEmpty()) {
            FOUND.put(dimension, reset);
        }
    }

    /**
     * Records the reset chunks found as levels loaded (see {@link #findResets}), firing
     * {@link ResourceZoneEvents#RESET} if there were any.
     */
    public static void recordFoundResets(MinecraftServer server) {
        recordReset(server, Map.copyOf(FOUND));
        FOUND.clear();
    }

    /**
     * Records a reset of the given chunks, firing {@link ResourceZoneEvents#RESET}, if there are
     * any.
     */
    public static void recordReset(MinecraftServer server, Map<ResourceKey<Level>, Set<ChunkPos>> chunks) {
        if (chunks.values().stream().allMatch(Set::isEmpty)) {
            return;
        }

        HISTORY.update(server, history -> history.withReset(chunks));
        ResourceZoneEvents.RESET.invoker().onReset(server, chunks);
    }

    /**
     * Moves the player (and anything they're riding) to their respawn point (or world spawn, if
     * they don't have one, or it's gone) if the chunk they're in was reset since they were last
     * here (see {@link #markResetsSeen}), and tells them why.
     */
    public static void moveIfChunkReset(ServerPlayer player, ResourceZonesConfig.Reset config) {
        final var level = player.level();
        final var history = HISTORY.get(level.getServer());
        if (history.wasResetSince(level.dimension(), player.chunkPosition(), SEEN.get(player))) {
            // Without using up a respawn anchor's charge
            final var respawn = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);

            // A mount they're riding was saved with them, so it survived the reset, and comes too,
            // with them still riding it
            player.getRootVehicle().teleport(respawn);

            final var toRespawnPoint = player.getRespawnConfig() != null && !respawn.missingRespawnBlock();
            final var message = toRespawnPoint ? config.movedToRespawnPoint() : config.movedToSpawn();
            player.sendSystemMessage(message.format(Map.of()));
        }
    }

    /**
     * Notes that the player has seen every reset so far, e.g. once they've joined, so they're only
     * moved for resets after this.
     */
    public static void markResetsSeen(ServerPlayer player) {
        SEEN.set(player, HISTORY.get(player.level().getServer()).resets());
    }

    /**
     * Forgets any reset chunks found but not yet recorded, e.g. because the server stopped.
     */
    public static void clear() {
        FOUND.clear();
    }
}
