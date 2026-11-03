package dev.seeruk.bundle.core.command;

import com.mojang.brigadier.Command;
import dev.seeruk.bundle.core.SeersCore;
import net.minecraft.commands.CommandSourceStack;

public final class CommandExecutors {
    private CommandExecutors() {}

    public static <T extends CommandSourceStack> Command<T> logged(final Command<T> command) {
        return context -> {
            try {
                return command.run(context);
            } catch (Exception e) {
                SeersCore.LOGGER.error("Error executing command", e);
                throw e;
            }
        };
    }
}
