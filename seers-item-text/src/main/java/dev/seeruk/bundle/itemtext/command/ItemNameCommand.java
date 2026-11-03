package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /itemname}: commands and subcommands for modifying item names.
 */
public class ItemNameCommand {
    static final String NAME = "itemname";

    private ItemNameCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME)
            .then(Commands.literal("set")
                .then(Commands.argument("name", nameArg())
                    .executes(ItemNameCommand::set)))
            .then(Commands.literal("reset")
                .executes(ItemNameCommand::reset)));
    }

    private static int set(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static StringArgumentType nameArg() {
        return StringArgumentType.greedyString();
    }
}
