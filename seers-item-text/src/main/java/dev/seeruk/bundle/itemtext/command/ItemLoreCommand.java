package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.itemtext.*;
import dev.seeruk.bundle.itemtext.dialog.LoreDialog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;

import static dev.seeruk.bundle.core.command.CommandReplies.fail;
import static dev.seeruk.bundle.core.command.CommandReplies.succeed;

/**
 * {@code /itemlore}: commands and subcommands for modifying item lore.
 */
public final class ItemLoreCommand {
    static final String NAME = "itemlore";

    private ItemLoreCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(
            dispatcher, SeersItemText.MOD_ID, NAME, name -> Commands.literal(name)
                .executes(CommandExecutors.logged(ItemLoreCommand::openEditor))
                .then(Commands.literal("edit")
                    .executes(CommandExecutors.logged(ItemLoreCommand::openEditor)))
                .then(Commands.literal("remove")
                    .executes(CommandExecutors.logged(ItemLoreCommand::remove)))
        );
    }

    private static int openEditor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var stack = EditableItem.inMainHand(player, context.getSource()::sendFailure);
        if (stack.isEmpty()) {
            return 0;
        }

        LoreDialog.open(player, stack.get());
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return edit(context, LoreEditor::remove);
    }

    private static int edit(
        CommandContext<CommandSourceStack> context,
        BiFunction<Player, ItemStack, LoreEditResult> callback
    ) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var stack = EditableItem.inMainHand(player, context.getSource()::sendFailure);
        if (stack.isEmpty()) {
            return 0;
        }

        final var result = callback.apply(player, stack.get());
        final var message = LoreEditMessages.of(result);
        if (result instanceof LoreEditResult.Edited) {
            return succeed(context.getSource(), message);
        }

        return fail(context.getSource(), message);
    }
}
