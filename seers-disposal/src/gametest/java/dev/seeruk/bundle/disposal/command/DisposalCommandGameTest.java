package dev.seeruk.bundle.disposal.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.disposal.DisposalMenu;
import dev.seeruk.bundle.disposal.SeersDisposal;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class DisposalCommandGameTest {
    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(
            root.getChild(DisposalCommand.NAME) != null,
            "Command not registered as " + DisposalCommand.NAME
        );

        helper.assertTrue(
            root.getChild(SeersDisposal.MOD_ID + ":" + DisposalCommand.NAME) != null,
            "Command not registered as " + SeersDisposal.MOD_ID + ":" + DisposalCommand.NAME
        );

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void opensDisposalMenu(GameTestHelper helper) throws CommandSyntaxException {
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        final var player = helper.makeMockServerPlayerInLevel();

        final var result = dispatcher.execute(DisposalCommand.NAME, player.createCommandSourceStack());

        helper.assertValueEqual(result, 1, "command result");
        helper.assertTrue(player.containerMenu instanceof DisposalMenu, "Menu wasn't opened");

        player.closeContainer();
        helper.succeed();
    }

    @GameTest
    public void refusesSpectators(GameTestHelper helper) throws CommandSyntaxException {
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        final var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);

        final var result = dispatcher.execute(DisposalCommand.NAME, player.createCommandSourceStack().withSuppressedOutput());

        helper.assertValueEqual(result, 0, "command result");
        // player.containerMenu can't be null, and is set to their inventory when one isn't open
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "A menu was opened for a spectator");

        helper.succeed();
    }

    @GameTest
    public void failsForNonPlayer(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(DisposalCommand.NAME, source);
            helper.fail("Command didn't fail for a non-player");
        } catch (CommandSyntaxException e) {
            helper.succeed();
        }
    }
}
