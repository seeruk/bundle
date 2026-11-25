package dev.seeruk.bundle.leaders.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.leaders.LeaderMannequins;
import dev.seeruk.bundle.leaders.LeadersConfig;
import dev.seeruk.bundle.leaders.PlaceResult;
import dev.seeruk.bundle.leaders.RemoveResult;
import dev.seeruk.bundle.leaders.Seat;
import dev.seeruk.bundle.leaders.SeersLeaders;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.function.Predicate;

/**
 * {@code /leaders}: placing and removing leader mannequins.
 */
public final class LeadersCommand {
    static final String NAME = "leaders";
    static final int MAX_RANK = 100;

    private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    private LeadersCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, LeadersConfig config) {
        NamespacedCommands.register(
            dispatcher, SeersLeaders.MOD_ID, NAME, name -> Commands.literal(name)
                .requires(ADMIN)
                .then(Commands.literal("place")
                    .then(Commands.argument("category", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            config.categories().stream().map(LeadersConfig.Category::id),
                            builder
                        ))
                        .executes(CommandExecutors.logged(context -> place(context, config, 1)))
                        .then(Commands.argument("rank", IntegerArgumentType.integer(1, MAX_RANK))
                            .executes(CommandExecutors.logged(context ->
                                place(context, config, IntegerArgumentType.getInteger(context, "rank")))))))
                .then(Commands.literal("remove")
                    .executes(CommandExecutors.logged(LeadersCommand::remove)))
        );
    }

    private static int place(CommandContext<CommandSourceStack> context, LeadersConfig config, int rank)
        throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        final var seat = new Seat(StringArgumentType.getString(context, "category"), rank);

        return switch (LeaderMannequins.place(player, config, seat)) {
            case PlaceResult.Placed _ -> {
                context.getSource().sendSuccess(() -> TextParsers.parseAsTrusted(
                    "<green>Placed a leader mannequin for %s #%d</green>".formatted(seat.category(), seat.rank())
                ), false);
                yield 1;
            }
            case PlaceResult.UnknownCategory failure -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>There's no category called %s</red>".formatted(failure.category())
                ));
                yield 0;
            }
            case PlaceResult.NoBlockInSight _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>Look at the block to place the mannequin on</red>"
                ));
                yield 0;
            }
            case PlaceResult.Obstructed _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>There isn't room for a mannequin there</red>"
                ));
                yield 0;
            }
        };
    }

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();

        return switch (LeaderMannequins.remove(player)) {
            case RemoveResult.Removed removed -> {
                context.getSource().sendSuccess(() -> TextParsers.parseAsTrusted(
                    "<green>Removed the leader mannequin for %s #%d</green>".formatted(
                        removed.seat().category(),
                        removed.seat().rank()
                    )
                ), false);
                yield 1;
            }
            case RemoveResult.NotLookingAtLeaderMannequin _ -> {
                context.getSource().sendFailure(TextParsers.parseAsTrusted(
                    "<red>Look at the leader mannequin to remove</red>"
                ));
                yield 0;
            }
        };
    }
}
