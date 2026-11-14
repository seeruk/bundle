package dev.seeruk.bundle.afk;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;

import static dev.seeruk.bundle.afk.AfkFixtures.player;

public class ActivityGameTest {
    @GameTest
    public void lookingAroundCounts(GameTestHelper helper) {
        final var player = loaded(player(helper));
        final var before = player.getLastActionTime();

        // A tick later, so a new action happens at a different time
        helper.runAfterDelay(1, () -> {
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(player.getYRot() + 10, player.getXRot(), true, false));

            helper.assertTrue(player.getLastActionTime() != before, "Looking around didn't count");
            helper.succeed();
        });
    }

    @GameTest
    public void beingMovedDoesntCount(GameTestHelper helper) {
        final var player = loaded(player(helper));
        final var before = player.getLastActionTime();
        final var to = player.position().add(0.1, 0, 0);

        helper.runAfterDelay(1, () -> {
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(to, true, false));

            helper.assertValueEqual(player.position(), to, "position");
            helper.assertValueEqual(player.getLastActionTime(), before, "last action time");
            helper.succeed();
        });
    }

    /**
     * Does what a client does after joining, so the server accepts its movement.
     */
    private static ServerPlayer loaded(ServerPlayer player) {
        // Joining teleports the player, which is the connection's first teleport
        player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(
            1, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()
        ));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return player;
    }
}
