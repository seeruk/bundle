package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.itemtext.EditableItem;
import dev.seeruk.bundle.itemtext.NameEditResult;
import dev.seeruk.bundle.itemtext.NameEditor;
import dev.seeruk.bundle.itemtext.SeersItemText;
import dev.seeruk.bundle.text.TextComponents;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;

import static dev.seeruk.bundle.core.command.CommandReplies.fail;
import static dev.seeruk.bundle.core.command.CommandReplies.succeed;

/**
 * {@code /itemname}: commands and subcommands for modifying item names.
 */
public final class ItemNameCommand {
    static final String NAME = "itemname";

    private ItemNameCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(
            dispatcher, SeersItemText.MOD_ID, NAME, name -> Commands.literal(name)
                .then(Commands.literal("set")
                    .then(Commands.argument("name", nameArg())
                        .executes(CommandExecutors.logged(ItemNameCommand::set))))
                .then(Commands.literal("reset")
                    .executes(CommandExecutors.logged(ItemNameCommand::reset)))
        );
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var name = StringArgumentType.getString(context, "name");
        return edit(context, (player, stack) -> NameEditor.set(player, stack, name));
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return edit(context, NameEditor::reset);
    }

    private static StringArgumentType nameArg() {
        return StringArgumentType.greedyString();
    }

    private static int edit(
        CommandContext<CommandSourceStack> context,
        BiFunction<Player, ItemStack, NameEditResult> callback
    ) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var stack = EditableItem.inMainHand(player, context.getSource()::sendFailure);
        if (stack.isEmpty()) {
            return 0;
        }

        final var result = callback.apply(player, stack.get());

        return switch (result) {
            case NameEditResult.Edited _ -> succeed(
                context.getSource(), Component.empty()
                    .append(TextParsers.parseAsTrusted("<green>Name updated:</green>\n"))
                    .append(stack.get().getStyledHoverName())
            );
            case NameEditResult.BlankName _ -> fail(
                context.getSource(), TextParsers.parseAsTrusted(
                    "<red>New name cannot be blank</red>"
                )
            );
            case NameEditResult.MaxLengthExceeded failure -> fail(
                context.getSource(), TextParsers.parseAsTrusted(
                    "<red>New name cannot be longer than %d characters (yours is %d)</red>".formatted(
                        failure.maxNameLength(),
                        TextComponents.length(failure.name().getString())
                    )
                )
            );
            case NameEditResult.NoCustomName _ -> fail(
                context.getSource(), TextParsers.parseAsTrusted(
                    "<red>This item has no custom name</red>"
                )
            );
        };
    }
}
