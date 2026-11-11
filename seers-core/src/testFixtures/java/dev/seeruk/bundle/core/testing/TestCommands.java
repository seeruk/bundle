package dev.seeruk.bundle.core.testing;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * A utility for running commands as a player in GameTests, so command tests don't each need their
 * own copy of the dispatcher boilerplate.
 */
public final class TestCommands {
    private TestCommands() {}

    /**
     * Runs the given command (without a leading {@code /}) as the player, with feedback suppressed so
     * it doesn't clutter the test logs.
     *
     * @return The command's result, i.e. what its executor returned.
     */
    public static int execute(GameTestHelper helper, ServerPlayer player, String command) throws CommandSyntaxException {
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        return dispatcher.execute(command, player.createCommandSourceStack().withSuppressedOutput());
    }
}
