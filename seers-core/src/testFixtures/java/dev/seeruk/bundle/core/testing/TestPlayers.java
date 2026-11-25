package dev.seeruk.bundle.core.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test utilities for mock players for GameTests.
 */
public final class TestPlayers {
    // Each player's connection, which keeps what's sent to them, by UUID. Concurrent, as tests run
    // alongside each other.
    private static final Map<UUID, EmbeddedChannel> CHANNELS = new ConcurrentHashMap<>();

    private TestPlayers() {}

    /**
     * The chat messages sent to the player by the server (e.g. announcements) since this was last
     * called for them, as plain text, oldest first. Messages from players and action bar messages
     * aren't included.
     */
    public static List<String> systemMessages(ServerPlayer player) {
        final var channel = CHANNELS.get(player.getUUID());
        final var messages = new ArrayList<String>();
        for (Object packet = channel.readOutbound(); packet != null; packet = channel.readOutbound()) {
            if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay()) {
                messages.add(chat.content().getString());
            }
        }
        return messages;
    }

    /**
     * Returns a survival mode player in the test's level, with a unique name, set up the same way
     * that the normal {@link GameTestHelper#makeMockServerPlayerInLevel()} does. Their client has
     * finished joining, so the server accepts their movement, and they can be hurt.
     */
    public static ServerPlayer player(GameTestHelper helper) {
        final var id = UUID.randomUUID();
        return player(helper, new NameAndId(id, "test_" + id.toString().substring(0, 8)));
    }

    /**
     * Like {@link #player(GameTestHelper)}, with a given UUID and name, e.g. so something can be
     * set up for the player before they join, or so they can rejoin with what was saved when they
     * left. The name should be unique, so the player doesn't share scores or name lookups with
     * another.
     */
    public static ServerPlayer player(GameTestHelper helper, NameAndId nameAndId) {
        final var cookie = CommonListenerCookie.createInitial(new GameProfile(nameAndId.id(), nameAndId.name()), false);
        final var server = helper.getLevel().getServer();
        final var player = new ServerPlayer(
            server,
            helper.getLevel(),
            cookie.gameProfile(),
            cookie.clientInformation()
        );

        // What Vanilla loads for a player who's played before, before they join
        // (see Vanilla's: PrepareSpawnTask.start)
        server.getPlayerList().loadPlayerData(nameAndId)
            .map(tag -> TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), tag))
            .ifPresent(player::load);

        final var connection = new Connection(PacketFlow.SERVERBOUND);
        CHANNELS.put(nameAndId.id(), new EmbeddedChannel(connection));
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
