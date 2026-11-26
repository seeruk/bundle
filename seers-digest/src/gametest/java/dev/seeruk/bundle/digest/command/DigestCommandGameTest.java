package dev.seeruk.bundle.digest.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.digest.SeersDigest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestCommands.executeAsAdmin;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class DigestCommandGameTest {
    // The command uses the server's config and schedule, which other tests don't touch

    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(root.getChild(DigestCommand.NAME) != null, "Command not registered as " + DigestCommand.NAME);
        helper.assertTrue(
            root.getChild(SeersDigest.MOD_ID + ":" + DigestCommand.NAME) != null,
            "Command not registered as " + SeersDigest.MOD_ID + ":" + DigestCommand.NAME
        );
        helper.succeed();
    }

    @GameTest
    public void publishAndListSucceed(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " publish"), 1, "publish result");
        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " list"), 1, "list result");
        helper.succeed();
    }

    @GameTest
    public void isOnlyForAdmins(GameTestHelper helper) {
        final var player = player(helper);

        try {
            execute(helper, player, DigestCommand.NAME + " list");
            helper.fail("Non-admin could use the command");
        } catch (CommandSyntaxException e) {
            // Expected
        }

        helper.succeed();
    }
}
