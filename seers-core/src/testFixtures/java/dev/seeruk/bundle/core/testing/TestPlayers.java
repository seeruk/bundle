package dev.seeruk.bundle.core.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Test utilities for mock players for GameTests.
 */
public final class TestPlayers {
    private TestPlayers() {}

    /**
     * Returns a survival mode player in the test's level, with a unique name, set up the same way
     * that the normal {@link GameTestHelper#makeMockServerPlayerInLevel()} does. Their client has
     * finished joining, so the server accepts their movement, and they can be hurt.
     */
    public static ServerPlayer player(GameTestHelper helper) {
        final var id = UUID.randomUUID();
        final var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "test_" + id.toString().substring(0, 8)), false);
        final var server = helper.getLevel().getServer();
        final var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());

        final var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        // The GameTest server's default game mode is creative
        player.setGameMode(GameType.SURVIVAL);

        // What a client does after joining. Joining teleports the player, which is the connection's
        // first teleport.
        player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(
            1, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
        ));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());

        return player;
    }
}
