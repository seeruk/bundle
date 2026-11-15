package dev.seeruk.bundle.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.SeersCore;
import net.minecraft.commands.CommandSourceStack;

public final class CommandExecutors {
    private CommandExecutors() {}

    /**
     * Wraps the command so any unexpected exception is logged before it's rethrown. Syntax
     * exceptions are ordinary failures (e.g. a command a player has to run, run from the console),
     * which Vanilla already reports to whoever ran it, so they're rethrown without logging.
     */
    public static <T extends CommandSourceStack> Command<T> logged(final Command<T> command) {
        return context -> {
            try {
                return command.run(context);
            } catch (CommandSyntaxException e) {
                throw e;
            } catch (Exception e) {
                SeersCore.LOGGER.error("Error executing command", e);
                throw e;
            }
        };
    }
}
