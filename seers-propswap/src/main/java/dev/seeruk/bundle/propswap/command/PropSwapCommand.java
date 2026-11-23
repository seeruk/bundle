package dev.seeruk.bundle.propswap.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.propswap.PropSwapMode;
import dev.seeruk.bundle.propswap.SeersPropSwap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /propswap}: toggles PropSwap for the player running it.
 */
public final class PropSwapCommand {
    static final String NAME = "propswap";

    private PropSwapCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(
            dispatcher, SeersPropSwap.MOD_ID, NAME, name -> Commands.literal(name)
                .executes(CommandExecutors.logged(PropSwapCommand::toggle))
        );
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var enabled = PropSwapMode.toggle(player);

        context.getSource().sendSuccess(
            () -> Component.literal(enabled ? "PropSwap enabled" : "PropSwap disabled"),
            false
        );

        return 1;
    }
}
