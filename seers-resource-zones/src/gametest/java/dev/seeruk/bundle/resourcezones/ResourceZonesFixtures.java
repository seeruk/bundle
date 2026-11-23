package dev.seeruk.bundle.resourcezones;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * Shared helpers for Resource Zones' GameTests. Zones are shared by the whole test server, so each
 * test should use its own chunks.
 */
public final class ResourceZonesFixtures {
    private ResourceZonesFixtures() {}

    /**
     * Adds the chunk to the resource zones in the player's dimension, and returns it (e.g. to move
     * the player there).
     */
    public static ChunkPos addZoneChunk(GameTestHelper helper, ServerPlayer player, ChunkPos chunk) {
        ResourceZones.add(helper.getLevel().getServer(), player.level().dimension(), chunk);
        return chunk;
    }

    /**
     * Moves the player to the middle of the chunk.
     */
    public static void moveTo(ServerPlayer player, ChunkPos chunk) {
        player.teleportTo(chunk.getMiddleBlockX(), 100, chunk.getMiddleBlockZ());
    }

    /**
     * Kills the player, then respawns them as a client does, returning the new player they become.
     */
    public static ServerPlayer respawn(GameTestHelper helper, ServerPlayer player) {
        player.kill(helper.getLevel());
        player.connection.handleClientCommand(
            new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)
        );

        final var respawned = helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID());
        helper.assertTrue(respawned != player, "Player didn't respawn");
        return respawned;
    }
}
