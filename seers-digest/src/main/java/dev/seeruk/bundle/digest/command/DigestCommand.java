package dev.seeruk.bundle.digest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.digest.Archive;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.IssueSchedule;
import dev.seeruk.bundle.digest.SeersDigest;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.book.BookConfig;
import dev.seeruk.bundle.digest.book.DigestBooks;
import dev.seeruk.bundle.digest.book.IssueDates;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.Prediction;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.seeruk.bundle.core.command.CommandReplies.fail;
import static dev.seeruk.bundle.core.command.CommandReplies.succeed;
import static dev.seeruk.bundle.core.command.CommandReplies.succeedAndBroadcast;

/**
 * {@code /digest}: managing the digest's issues.
 */
public final class DigestCommand {
    static final String NAME = "digest";

    private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    private DigestCommand() {}

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        DigestConfig config,
        BookConfig book,
        Clock clock
    ) {
        NamespacedCommands.register(
            dispatcher, SeersDigest.MOD_ID, NAME, name -> Commands.literal(name)
                .requires(ADMIN)
                .then(Commands.literal("publish")
                    .executes(CommandExecutors.logged(context -> publish(context, config, clock))))
                .then(Commands.literal("list")
                    .executes(CommandExecutors.logged(context -> list(context, clock))))
                .then(Commands.literal("book")
                    .executes(CommandExecutors.logged(context -> giveBook(context, config, book, Optional.empty())))
                    .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .executes(CommandExecutors.logged(context -> giveBook(
                            context,
                            config,
                            book,
                            Optional.of(IntegerArgumentType.getInteger(context, "number"))
                        )))))
        );
    }

    private static int publish(CommandContext<CommandSourceStack> context, DigestConfig config, Clock clock) {
        final var source = context.getSource();
        return IssueSchedule.publishNow(source.getServer(), config, clock)
            .map(issue -> succeedAndBroadcast(
                source, TextParsers.parseAsTrusted(
                    "<green>Published issue #%d</green>".formatted(issue.number())
                )
            ))
            .orElseGet(() -> succeedAndBroadcast(
                source, TextParsers.parseAsTrusted(
                    "<yellow>Nobody's been active since the last issue, so there's nothing to publish</yellow>"
                )
            ));
    }

    // Gives the player running it the numbered issue's book, or the latest issue's
    private static int giveBook(
        CommandContext<CommandSourceStack> context,
        DigestConfig config,
        BookConfig book,
        Optional<Integer> number
    )
        throws CommandSyntaxException {
        final var source = context.getSource();
        final var player = source.getPlayerOrException();
        final var issue = number
            .map(found -> Archive.get(source.getServer(), found))
            .orElseGet(() -> Archive.latest(source.getServer()));

        if (issue.isEmpty()) {
            return fail(
                source, TextParsers.parseAsTrusted(number
                    .map("<red>There's no issue #%d</red>"::formatted)
                    .orElse("<red>No issues have been published yet</red>"))
            );
        }

        // Dropped at their feet if their inventory is full
        player.getInventory().placeItemBackInInventory(
            DigestBooks.create(source.getServer(), config, book, issue.get()),
            Prediction.SERVER_ONLY
        );
        return succeed(
            source, TextParsers.parseAsTrusted("<green>Gave you issue #%d</green>".formatted(issue.get().number()))
        );
    }

    private static int list(CommandContext<CommandSourceStack> context, Clock clock) {
        final var issues = Archive.all(context.getSource().getServer());
        if (issues.isEmpty()) {
            return succeed(
                context.getSource(), TextParsers.parseAsTrusted(
                    "<yellow>No issues have been published yet</yellow>"
                )
            );
        }

        final var lines = issues.reversed().stream()
            .map(issue -> describe(issue, clock.getZone()))
            .collect(Collectors.joining("\n"));
        return succeed(context.getSource(), TextParsers.parseAsTrusted(lines));
    }

    // e.g. "#3: 16 to 23 Nov 2026, 12 players (2 new)"
    static String describe(Issue issue, ZoneId zone) {
        return "<gold>#%d</gold>: %s, %d %s (%d new)".formatted(
            issue.number(),
            IssueDates.between(issue.started(), issue.ended(), zone),
            issue.activePlayers().size(),
            issue.activePlayers().size() == 1 ? "player" : "players",
            issue.newPlayers().size()
        );
    }
}
