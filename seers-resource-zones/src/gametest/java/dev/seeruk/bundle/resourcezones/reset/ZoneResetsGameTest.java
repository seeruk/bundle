package dev.seeruk.bundle.resourcezones.reset;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.addZoneChunk;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

// Zones are shared by the whole test server, so each test uses its own chunks
public class ZoneResetsGameTest {
    private static final ResourceZonesConfig.Reset CONFIG = new ResourceZonesConfig.Reset(
        TextTemplate.parse("Moved to your respawn point"),
        TextTemplate.parse("Moved to spawn")
    );

    private static final EventLog<Map<ResourceKey<Level>, Set<ChunkPos>>> RESETS = new EventLog<>();

    static {
        ResourceZoneEvents.RESET.register((server, chunks) -> RESETS.record(chunks));
    }

    @GameTest
    public void remembersLoadedZoneChunksAsGenerated(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = addZoneChunk(helper, player, new ChunkPos(9000, 0));

        helper.getLevel().getChunk(chunk.x(), chunk.z());

        helper.assertTrue(generated(helper, chunk), "Chunk wasn't remembered as generated");
        helper.succeed();
    }

    @GameTest
    public void doesntRememberChunksOutsideZones(GameTestHelper helper) {
        final var chunk = new ChunkPos(9010, 0);

        helper.getLevel().getChunk(chunk.x(), chunk.z());

        helper.assertFalse(generated(helper, chunk), "Chunk was remembered as generated");
        helper.succeed();
    }

    @GameTest
    public void forgetsChunksRemovedFromZones(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = addZoneChunk(helper, player, new ChunkPos(9020, 0));
        helper.getLevel().getChunk(chunk.x(), chunk.z());
        helper.assertTrue(generated(helper, chunk), "Chunk wasn't remembered as generated");

        ResourceZones.remove(helper.getLevel().getServer(), Level.OVERWORLD, chunk);

        helper.assertFalse(generated(helper, chunk), "Chunk was still remembered as generated");
        helper.succeed();
    }

    @GameTest
    public void resettingFiresOneEventWithTheChunks(GameTestHelper helper) {
        final var chunks = Map.of(Level.OVERWORLD, Set.of(new ChunkPos(9030, 0), new ChunkPos(9031, 0)));
        final var mark = RESETS.mark();

        ZoneResets.reset(helper.getLevel().getServer(), chunks);

        helper.assertValueEqual(RESETS.matchingSince(mark, reset -> true), List.of(chunks), "resets");
        helper.succeed();
    }

    @GameTest
    public void resettingNothingChangesNothing(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var resets = ZoneResets.history(server).resets();
        final var mark = RESETS.mark();

        ZoneResets.reset(server, Map.of(Level.OVERWORLD, Set.of()));

        helper.assertValueEqual(RESETS.matchingSince(mark, reset -> true), List.of(), "resets");
        helper.assertValueEqual(ZoneResets.history(server).resets(), resets, "reset count");
        helper.succeed();
    }

    @GameTest
    public void movesPlayersInAChunkResetSinceTheyLeftToSpawn(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = new ChunkPos(9040, 0);
        moveTo(player, chunk);
        ZoneResets.reset(helper.getLevel().getServer(), Map.of(Level.OVERWORLD, Set.of(chunk)));

        ZoneResets.rejoined(player, CONFIG);

        // Spawning can put players anywhere within the spawn radius
        final var spawn = helper.getLevel().getServer().getRespawnData().pos();
        helper.assertTrue(player.blockPosition().closerThan(spawn, 64), "Player wasn't moved to spawn");
        helper.succeed();
    }

    @GameTest
    public void movesPlayersToTheirRespawnPoint(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = new ChunkPos(9050, 0);
        final var respawnPoint = new BlockPos(9051 * 16 + 8, 200, 8);
        player.setRespawnPosition(
            new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(Level.OVERWORLD, respawnPoint, 0, 0), true),
            false
        );
        moveTo(player, chunk);
        ZoneResets.reset(helper.getLevel().getServer(), Map.of(Level.OVERWORLD, Set.of(chunk)));

        ZoneResets.rejoined(player, CONFIG);

        helper.assertValueEqual(player.chunkPosition(), ChunkPos.containing(respawnPoint), "player's chunk");
        helper.succeed();
    }

    @GameTest
    public void movesTheMountPlayersAreRiding(GameTestHelper helper) {
        // Vanilla saves a mount with the player riding it, so it survives the reset
        final var player = player(helper);
        final var chunk = new ChunkPos(9080, 0);
        final var respawnPoint = new BlockPos(9081 * 16 + 8, 200, 8);
        player.setRespawnPosition(
            new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(Level.OVERWORLD, respawnPoint, 0, 0), true),
            false
        );
        final var horse = ride(helper, player, chunk);
        ZoneResets.reset(helper.getLevel().getServer(), Map.of(Level.OVERWORLD, Set.of(chunk)));

        ZoneResets.rejoined(player, CONFIG);

        helper.assertTrue(player.getVehicle() == horse, "Player wasn't still riding");
        helper.assertValueEqual(horse.chunkPosition(), ChunkPos.containing(respawnPoint), "mount's chunk");
        helper.succeed();
    }

    @GameTest
    public void leavesPlayersInAChunkResetBeforeTheyLastJoined(GameTestHelper helper) {
        // Seeing the reset happens when they join
        final var chunk = new ChunkPos(9060, 0);
        ZoneResets.reset(helper.getLevel().getServer(), Map.of(Level.OVERWORLD, Set.of(chunk)));
        final var player = player(helper);
        moveTo(player, chunk);

        ZoneResets.rejoined(player, CONFIG);

        helper.assertValueEqual(player.chunkPosition(), chunk, "player's chunk");
        helper.succeed();
    }

    @GameTest
    public void onlyMovesPlayersOncePerReset(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = new ChunkPos(9070, 0);
        moveTo(player, chunk);
        ZoneResets.reset(helper.getLevel().getServer(), Map.of(Level.OVERWORLD, Set.of(chunk)));
        ZoneResets.rejoined(player, CONFIG);

        moveTo(player, chunk);
        ZoneResets.rejoined(player, CONFIG);

        helper.assertValueEqual(player.chunkPosition(), chunk, "player's chunk");
        helper.succeed();
    }

    /**
     * Puts the player on a horse in the middle of the chunk, returning the horse.
     */
    private static Horse ride(GameTestHelper helper, ServerPlayer player, ChunkPos chunk) {
        final var level = helper.getLevel();
        level.getChunk(chunk.x(), chunk.z());
        moveTo(player, chunk);

        final var horse = EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND);
        helper.assertTrue(horse != null, "Horse wasn't created");
        horse.snapTo(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(horse);
        helper.assertTrue(player.startRiding(horse, true, false), "Player couldn't ride the horse");
        return horse;
    }

    private static boolean generated(GameTestHelper helper, ChunkPos chunk) {
        return ZoneResets.history(helper.getLevel().getServer()).generated().contains(Level.OVERWORLD, chunk);
    }
}
