package dev.seeruk.bundle.core.command;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Utilities for replying to whoever ran a command. Each returns the command's result, so a reply
 * can be the last thing a command does, e.g. {@code return fail(source, message);}.
 */
public final class CommandReplies {
    private CommandReplies() {}

    /**
     * Tells whoever ran a command that it worked.
     */
    public static int succeed(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Tells whoever ran a command that it worked, and broadcasts to admins online and the server.
     */
    public static int succeedAndBroadcast(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Tells whoever ran a command that it didn't work.
     */
    public static int fail(CommandSourceStack source, Component message) {
        source.sendFailure(message);
        return 0;
    }
}
