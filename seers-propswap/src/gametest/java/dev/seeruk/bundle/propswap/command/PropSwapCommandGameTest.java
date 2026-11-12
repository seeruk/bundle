package dev.seeruk.bundle.propswap.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.propswap.PropSwapMode;
import dev.seeruk.bundle.propswap.SeersPropSwap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.seeruk.bundle.core.testing.TestCommands.execute;

public class PropSwapCommandGameTest {
    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(
            root.getChild(PropSwapCommand.NAME) != null,
            "Command not registered as " + PropSwapCommand.NAME
        );

        helper.assertTrue(
            root.getChild(SeersPropSwap.MOD_ID + ":" + PropSwapCommand.NAME) != null,
            "Command not registered as " + SeersPropSwap.MOD_ID + ":" + PropSwapCommand.NAME
        );

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void togglesPropSwap(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();

        helper.assertValueEqual(execute(helper, player, PropSwapCommand.NAME), 1, "enable result");
        helper.assertTrue(PropSwapMode.isEnabled(player), "PropSwap wasn't enabled");

        helper.assertValueEqual(execute(helper, player, PropSwapCommand.NAME), 1, "disable result");
        helper.assertTrue(!PropSwapMode.isEnabled(player), "PropSwap wasn't disabled");

        helper.succeed();
    }

    @GameTest
    public void refusesNonPlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(PropSwapCommand.NAME, source);
            helper.fail("Command didn't fail for a non-player");
        } catch (CommandSyntaxException e) {
            helper.succeed();
        }
    }
}
