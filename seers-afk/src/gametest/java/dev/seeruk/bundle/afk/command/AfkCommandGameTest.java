package dev.seeruk.bundle.afk.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.afk.AfkFixtures.Changed;
import dev.seeruk.bundle.afk.AfkStatus;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static dev.seeruk.bundle.afk.AfkFixtures.eventsFor;
import static dev.seeruk.bundle.afk.SeersAfk.MOD_ID;
import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class AfkCommandGameTest {
    private static final int TIMEOUT = 100;

    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(root.getChild(AfkCommand.NAME) != null, "Command not registered as " + AfkCommand.NAME);
        helper.assertTrue(
            root.getChild(MOD_ID + ":" + AfkCommand.NAME) != null,
            "Command not registered as " + MOD_ID + ":" + AfkCommand.NAME
        );
        helper.succeed();
    }

    @GameTest
    public void togglesAfkStatus(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        execute(helper, player, AfkCommand.NAME);
        helper.assertTrue(AfkStatus.isAfk(player), "Player wasn't AFK");

        execute(helper, player, AfkCommand.NAME);
        helper.assertFalse(AfkStatus.isAfk(player), "Player was still AFK");

        helper.assertValueEqual(
            eventsFor(player), List.of(
                new Changed(player.getUUID(), true, AfkReason.COMMAND),
                new Changed(player.getUUID(), false, AfkReason.COMMAND)
            ), "events"
        );
        helper.succeed();
    }

    @GameTest
    public void comingBackWithCommandStartsIdleTimeAgain(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var tick = helper.getLevel().getServer().getTickCount();
        // Idle since a timeout ago, so they're AFK now. The command runs on the current tick.
        AfkStatus.update(player, tick - TIMEOUT, TIMEOUT);
        AfkStatus.update(player, tick, TIMEOUT);

        execute(helper, player, AfkCommand.NAME);

        AfkStatus.update(player, tick + TIMEOUT - 1, TIMEOUT);
        helper.assertFalse(AfkStatus.isAfk(player), "Player went AFK before the timeout");

        AfkStatus.update(player, tick + TIMEOUT, TIMEOUT);
        helper.assertTrue(AfkStatus.isAfk(player), "Player didn't go AFK after the timeout");
        helper.succeed();
    }

    @GameTest
    public void typingCommandDoesntCountAsActivity(GameTestHelper helper) {
        final var player = player(helper);
        final var tick = helper.getLevel().getServer().getTickCount();
        AfkStatus.update(player, tick - TIMEOUT, TIMEOUT);
        AfkStatus.update(player, tick, TIMEOUT);

        // Chat arrives on a network thread, and Vanilla runs the command later, on the server
        // thread. Activity counted on arrival would bring the player back before the command runs.
        CompletableFuture.runAsync(() ->
            player.connection.handleChatCommand(new ServerboundChatCommandPacket(AfkCommand.NAME))).join();

        helper.runAfterDelay(
            1, () -> {
                helper.assertValueEqual(
                    eventsFor(player), List.of(
                        new Changed(player.getUUID(), true, AfkReason.TIMEOUT),
                        new Changed(player.getUUID(), false, AfkReason.COMMAND)
                    ), "events"
                );
                helper.succeed();
            }
        );
    }
}
