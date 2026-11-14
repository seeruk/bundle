package dev.seeruk.bundle.afk.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.afk.AfkFixtures.Changed;
import dev.seeruk.bundle.afk.AfkStatus;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;

import static dev.seeruk.bundle.afk.AfkFixtures.eventsFor;
import static dev.seeruk.bundle.afk.AfkFixtures.player;
import static dev.seeruk.bundle.afk.SeersAFK.MOD_ID;
import static dev.seeruk.bundle.core.testing.TestCommands.execute;

public class AfkCommandGameTest {
    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(root.getChild(AfkCommand.NAME) != null, "Command not registered as " + AfkCommand.NAME);
        helper.assertTrue(root.getChild(MOD_ID + ":" + AfkCommand.NAME) != null, "Command not registered as " + MOD_ID + ":" + AfkCommand.NAME);
        helper.succeed();
    }

    @GameTest
    public void togglesAfkStatus(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        execute(helper, player, AfkCommand.NAME);
        helper.assertTrue(AfkStatus.isAfk(player), "Player wasn't AFK");

        execute(helper, player, AfkCommand.NAME);
        helper.assertFalse(AfkStatus.isAfk(player), "Player was still AFK");

        helper.assertValueEqual(eventsFor(player), List.of(
            new Changed(player.getUUID(), true, AfkReason.COMMAND),
            new Changed(player.getUUID(), false, AfkReason.COMMAND)
        ), "events");
        helper.succeed();
    }
}
