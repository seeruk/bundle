package dev.seeruk.bundle.afk.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.afk.AfkStatus;
import dev.seeruk.bundle.afk.api.AfkReason;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static dev.seeruk.bundle.afk.SeersAFK.MOD_ID;

/**
 * {@code /afk}: toggles the calling player's AFK status.
 */
public final class AfkCommand {
    static final String NAME = "afk";

    private AfkCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(dispatcher, MOD_ID, NAME, name -> Commands.literal(name)
            .executes(CommandExecutors.logged(AfkCommand::toggle)));
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AfkStatus.toggle(context.getSource().getPlayerOrException(), AfkReason.COMMAND);
        return 1;
    }

    /**
     * Whether a command a player typed (without the slash) is this one.
     */
    public static boolean isAfkCommand(String command) {
        final var name = command.split(" ", 2)[0];
        return name.equals(NAME) || name.equals(MOD_ID + ":" + NAME);
    }
}
