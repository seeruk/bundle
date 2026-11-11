package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.itemtext.LoreEditResult;
import dev.seeruk.bundle.itemtext.LoreEditor;
import dev.seeruk.bundle.itemtext.LoreLines;
import dev.seeruk.bundle.itemtext.SeersItemText;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * {@code /itemlore}: commands and subcommands for modifying item lore.
 */
public class ItemLoreCommand extends ItemTextCommand {
    static final String NAME = "itemlore";

    private ItemLoreCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(dispatcher, SeersItemText.MOD_ID, NAME, name -> Commands.literal(name)
            .executes(CommandExecutors.logged(ItemLoreCommand::list))
            .then(Commands.literal("add")
                .then(Commands.argument("lore", loreArg())
                    .executes(CommandExecutors.logged(ItemLoreCommand::add))))
            .then(Commands.literal("set")
                .then(Commands.argument("line", lineArg())
                    .then(Commands.argument("lore", loreArg())
                        .executes(CommandExecutors.logged(ItemLoreCommand::set)))))
            .then(Commands.literal("insert")
                .then(Commands.argument("line", lineArg())
                    .executes(CommandExecutors.logged(ItemLoreCommand::insertEmpty))
                    .then(Commands.argument("lore", loreArg())
                        .executes(CommandExecutors.logged(ItemLoreCommand::insert)))))
            .then(Commands.literal("remove")
                .executes(CommandExecutors.logged(ItemLoreCommand::removeAll))
                .then(Commands.argument("line", lineArg())
                    .executes(CommandExecutors.logged(ItemLoreCommand::remove))))
            .then(Commands.literal("clear")
                .then(Commands.argument("line", lineArg())
                    .executes(CommandExecutors.logged(ItemLoreCommand::clear))))
        );
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var stack = mainHandItem(context);
        if (stack.isEmpty()) {
            return 0;
        }

        context.getSource().sendSuccess(
            () -> Component.empty()
                .append(TextParsers.parseAsTrusted("<green>Current Lore:</green>\n"))
                .append(preview(LoreEditor.lines(stack.get()))),
            false
        );

        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var lore = StringArgumentType.getString(context, "lore");
        return edit(context, (player, stack) -> LoreEditor.add(player, stack, lore));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        final var lore = StringArgumentType.getString(context, "lore");
        return edit(context, (player, stack) -> LoreEditor.set(player, stack, line, lore));
    }

    private static int insert(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        final var lore = StringArgumentType.getString(context, "lore");
        return edit(context, (player, stack) -> LoreEditor.insert(player, stack, line, lore));
    }

    private static int insertEmpty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        return edit(context, (player, stack) -> LoreEditor.insert(player, stack, line, ""));
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        return edit(context, (player, stack) -> LoreEditor.remove(player, stack, line));
    }

    private static int removeAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return edit(context, LoreEditor::removeAll);
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        return edit(context, (player, stack) -> LoreEditor.clear(player, stack, line));
    }

    private static IntegerArgumentType lineArg() {
        return IntegerArgumentType.integer(1, LoreLines.MAX_LINES);
    }

    private static StringArgumentType loreArg() {
        return StringArgumentType.greedyString();
    }

    private static int edit(
        CommandContext<CommandSourceStack> context,
        BiFunction<Player, ItemStack, LoreEditResult> callback
    ) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var stack = mainHandItem(context);
        if (stack.isEmpty()) {
            return 0;
        }

        final var result = callback.apply(player, stack.get());

        return switch (result) {
            case LoreEditResult.Edited edited -> {
                context.getSource().sendSuccess(
                    () -> Component.empty()
                        .append(TextParsers.parseAsTrusted("<green>Lore updated:</green>\n"))
                        .append(preview(edited.edited())),
                    false
                );
                yield 1;
            }
            case LoreEditResult.MaxLineLengthExceeded failure -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>A line of lore cannot be longer than %d characters (yours is %d)</red>".formatted(
                        failure.maxLineLength(),
                        failure.line().getString().length()
                    )
                ));
                yield 0;
            }
            case LoreEditResult.LineOutOfBounds failure -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>Line %d doesn't exist (must be between 1 and %d)</red>".formatted(
                        failure.line(),
                        failure.currentLines()
                    )
                ));
                yield 0;
            }
            case LoreEditResult.MaxLinesExceeded failure -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>Cannot add more lore lines (max %d)</red>".formatted(
                        failure.maxLines()
                    )
                ));
                yield 0;
            }
            case LoreEditResult.NoLore _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>This item has no lore</red>"
                ));
                yield 0;
            }
        };
    }

    /**
     * Constructs a preview of the lore lines as a single component, ready to be printed
     * to the player after a command successfully modifies the lore.
     */
    private static Component preview(List<Component> lines) {
        final var styled = new ItemLore(lines).styledLines();
        if (styled.isEmpty()) {
            return Component.literal("No lore").withStyle(ChatFormatting.GRAY);
        }

        final var numbered = IntStream.range(0, styled.size())
            .mapToObj(i ->
                Component.empty()
                    .append(Component.literal((i + 1) + ": ").withStyle(ChatFormatting.GRAY))
                    .append(styled.get(i))
            )
            .toList();

        return ComponentUtils.formatList(numbered, Component.literal("\n"));
    }
}
