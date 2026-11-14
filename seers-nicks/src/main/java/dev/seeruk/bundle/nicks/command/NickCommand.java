package dev.seeruk.bundle.nicks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.nicks.NickEditResult;
import dev.seeruk.bundle.nicks.Nickname;
import dev.seeruk.bundle.nicks.Nicknames;
import dev.seeruk.bundle.nicks.SeersNicks;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.function.Function;

/**
 * {@code /nick}: commands for players to change their own nickname, and for admins to change anyone's.
 */
public final class NickCommand {
    static final String NAME = "nick";

    private NickCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(dispatcher, SeersNicks.MOD_ID, NAME, name -> Commands.literal(name)
            .then(Commands.literal("set")
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                    .executes(CommandExecutors.logged(NickCommand::set))))
            .then(Commands.literal("reset")
                .executes(CommandExecutors.logged(NickCommand::reset)))
            .then(Commands.literal("player")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                    .then(Commands.literal("set")
                        .then(Commands.argument("nickname", StringArgumentType.greedyString())
                            .executes(CommandExecutors.logged(NickCommand::setPlayer))))
                    .then(Commands.literal("reset")
                        .executes(CommandExecutors.logged(NickCommand::resetPlayer))))));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var input = StringArgumentType.getString(context, "nickname");
        final var trusted = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(context.getSource());
        final var target = new NameAndId(player.getUUID(), player.getGameProfile().name());

        return feedback(context, Nicknames.set(context.getSource().getServer(), target, input, trusted), target, true);
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var target = new NameAndId(player.getUUID(), player.getGameProfile().name());

        return feedback(context, Nicknames.reset(context.getSource().getServer(), target), target, true);
    }

    private static int setPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var input = StringArgumentType.getString(context, "nickname");
        return forPlayer(context, target -> Nicknames.set(context.getSource().getServer(), target, input, true));
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return forPlayer(context, target -> Nicknames.reset(context.getSource().getServer(), target));
    }

    private static int forPlayer(
        CommandContext<CommandSourceStack> context,
        Function<NameAndId, NickEditResult> edit
    ) throws CommandSyntaxException {
        final var targets = GameProfileArgument.getGameProfiles(context, "player");
        if (targets.size() != 1) {
            context.getSource().sendFailure(TextParsers.parseAsTrusted("<red>Choose a single player</red>"));
            return 0;
        }

        final var target = targets.iterator().next();
        return feedback(context, edit.apply(target), target, false);
    }

    private static int feedback(CommandContext<CommandSourceStack> context, NickEditResult result, NameAndId target, boolean own) {
        return switch (result) {
            case NickEditResult.Edited edited -> {
                final var whose = own ? "Your nickname" : target.name() + "'s nickname";
                final Component message = edited.edited()
                    .<Component>map(nickname -> Component.empty()
                        .append(TextParsers.parseAsTrusted("<green>" + whose + " is now:</green> "))
                        .append(Nickname.PREFIX)
                        .append(nickname))
                    .orElseGet(() -> TextParsers.parseAsTrusted("<green>" + whose + " has been reset</green>"));

                context.getSource().sendSuccess(() -> message, false);
                yield 1;
            }
            case NickEditResult.BlankName _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>Nicknames can't be blank</red>"
                ));
                yield 0;
            }
            case NickEditResult.MaxLengthExceeded failure -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>Nicknames can't be longer than %d characters (yours is %d)</red>".formatted(
                        failure.maxNameLength(),
                        failure.name().getString().length()
                    )
                ));
                yield 0;
            }
            case NickEditResult.UsernameTaken _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>That's another player's username</red>"
                ));
                yield 0;
            }
            case NickEditResult.NoNickname _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>" + (own ? "You don't" : target.name() + " doesn't") + " have a nickname</red>"
                ));
                yield 0;
            }
        };
    }
}
