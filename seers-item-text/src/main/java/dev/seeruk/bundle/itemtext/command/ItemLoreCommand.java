package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.Result;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.itemtext.LoreText;
import dev.seeruk.bundle.itemtext.SeersItemText;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * {@code /itemlore}: commands and subcommands for modifying item lore.
 */
public class ItemLoreCommand {
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
                .append(TextParsers.parseAsUntrusted("<green>Current Lore:</green>\n"))
                .append(preview(LoreText.lines(stack.get()))),
            false
        );

        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var lore = StringArgumentType.getString(context, "lore");
        return write(context, (player, stack) -> LoreText.add(player, stack, lore));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        final var lore = StringArgumentType.getString(context, "lore");
        return write(context, (player, stack) -> LoreText.set(player, stack, line, lore));
    }

    private static int insert(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        final var lore = StringArgumentType.getString(context, "lore");
        return write(context, (player, stack) -> LoreText.insert(player, stack, line, lore));
    }

    private static int insertEmpty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        return write(context, (player, stack) -> LoreText.insert(player, stack, line, ""));
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        return write(context, (player, stack) -> LoreText.remove(player, stack, line));
    }

    private static int removeAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return write(context, LoreText::removeAll);
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var line = IntegerArgumentType.getInteger(context, "line");
        return write(context, (player, stack) -> LoreText.clear(player, stack, line));
    }

    private static IntegerArgumentType lineArg() {
        return IntegerArgumentType.integer(1, LoreText.MAX_LINES);
    }

    private static StringArgumentType loreArg() {
        return StringArgumentType.greedyString();
    }

    private static int write(
        CommandContext<CommandSourceStack> context,
        BiFunction<Player, ItemStack, Result<List<Component>, String>> callback
    ) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var stack = mainHandItem(context);
        if (stack.isEmpty()) {
            return 0;
        }

        final var result = callback.apply(player, stack.get());

        return switch (result) {
            case Result.Success<List<Component>, String> lines -> {
                context.getSource().sendSuccess(
                    () -> Component.empty()
                        .append(TextParsers.parseAsUntrusted("<green>Lore updated:</green>\n"))
                        .append(preview(lines.value())),
                    false
                );
                yield 1;
            }
            case Result.Failure<List<Component>, String> failure -> {
                context.getSource().sendFailure(TextParsers.parseAsUntrusted("<red>" + failure.error() + "</red>"));
                yield 0;
            }
        };
    }

    /**
     * Constructs a preview of the lore lines as a single component, ready to be printed
     * to the player after a command successfully modifies the lore.
     */
    private static Component preview(List<Component> lines) {
        final var preview = Component.empty();
        final var styled = new ItemLore(lines).styledLines();

        if (styled.isEmpty()) {
            return preview.append(Component.literal("No lore").withStyle(ChatFormatting.GRAY));
        }

        for (var i = 0; i < styled.size(); i++) {
            preview.append(Component.literal((i + 1) + ": ").withStyle(ChatFormatting.GRAY))
                .append(styled.get(i))
                .append("\n");
        }

        return preview;
    }

    private static Optional<ItemStack> mainHandItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        if (player.isSpectator()) {
            context.getSource().sendFailure(TextParsers.parseAsUntrusted(
                "<red>You can't modify item lore while spectating!</red>"
            ));
            return Optional.empty();
        }

        final var stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(TextParsers.parseAsUntrusted(
                "<red>Hold an item in your main hand to modify its lore</red>"
            ));
            return Optional.empty();
        }

        return Optional.of(stack);
    }
}
