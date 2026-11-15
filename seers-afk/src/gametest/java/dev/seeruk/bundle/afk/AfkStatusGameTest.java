package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.AfkFixtures.Changed;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

import java.util.List;

import static dev.seeruk.bundle.afk.AfkFixtures.eventsFor;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class AfkStatusGameTest {
    private static final int TIMEOUT = 100;

    @GameTest
    public void playerGoesAfkAfterTheTimeout(GameTestHelper helper) {
        final var player = player(helper);
        final var tick = helper.getLevel().getServer().getTickCount();

        AfkStatus.update(player, tick, TIMEOUT);
        AfkStatus.update(player, tick + TIMEOUT, TIMEOUT);

        helper.assertTrue(AfkStatus.isAfk(player), "Player wasn't AFK");
        helper.assertValueEqual(eventsFor(player), List.of(new Changed(player.getUUID(), true, AfkReason.TIMEOUT)), "events");
        helper.succeed();
    }

    @GameTest
    public void playerIsntAfkBeforeTheTimeout(GameTestHelper helper) {
        final var player = player(helper);
        final var tick = helper.getLevel().getServer().getTickCount();

        AfkStatus.update(player, tick, TIMEOUT);
        AfkStatus.update(player, tick + TIMEOUT - 1, TIMEOUT);

        helper.assertFalse(AfkStatus.isAfk(player), "Player was AFK");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired");
        helper.succeed();
    }

    @GameTest
    public void doingSomethingBringsPlayerBack(GameTestHelper helper) {
        final var player = player(helper);
        final var tick = helper.getLevel().getServer().getTickCount();
        AfkStatus.update(player, tick, TIMEOUT);
        AfkStatus.update(player, tick + TIMEOUT, TIMEOUT);

        // A tick later, so the action happens at a different time to the one before going AFK
        helper.runAfterDelay(1, () -> {
            player.resetLastActionTime();
            AfkStatus.update(player, tick + TIMEOUT + 1, TIMEOUT);

            helper.assertFalse(AfkStatus.isAfk(player), "Player was still AFK");
            helper.assertValueEqual(eventsFor(player).getLast(), new Changed(player.getUUID(), false, AfkReason.ACTIVITY), "event");
            helper.succeed();
        });
    }

    @GameTest
    public void respawningKeepsPlayerAfk(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        AfkStatus.toggle(player, server.getTickCount(), AfkReason.COMMAND);

        // A tick later, so respawning happens at a different time to going AFK
        helper.runAfterDelay(1, () -> {
            player.kill(helper.getLevel());
            // What the client sends by itself with the doImmediateRespawn game rule
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        });

        // Another tick later, so statuses have been updated since the respawn
        helper.runAfterDelay(2, () -> {
            final var respawned = server.getPlayerList().getPlayer(player.getUUID());

            helper.assertTrue(respawned != player, "Player didn't respawn");
            helper.assertTrue(AfkStatus.isAfk(respawned), "Player wasn't AFK after respawning");
            helper.assertValueEqual(eventsFor(player).size(), 1, "event count");
            helper.succeed();
        });
    }

    @GameTest
    public void forgettingPlayerDoesntAnnounceIt(GameTestHelper helper) {
        final var player = player(helper);
        AfkStatus.toggle(player, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);

        AfkStatus.forget(player);

        helper.assertFalse(AfkStatus.isAfk(player), "Player was still AFK");
        helper.assertValueEqual(eventsFor(player).size(), 1, "event count");
        helper.succeed();
    }
}
