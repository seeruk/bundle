package dev.seeruk.bundle.digest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.digest.Archive;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.IssueSchedule;
import dev.seeruk.bundle.digest.SeersDigest;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.seeruk.bundle.core.command.CommandReplies.succeed;
import static dev.seeruk.bundle.core.command.CommandReplies.succeedAndBroadcast;

/**
 * {@code /digest}: managing the digest's issues.
 */
public final class DigestCommand {
    static final String NAME = "digest";

    private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ROOT);
    private static final DateTimeFormatter DAY_AND_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ROOT);

    private DigestCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, DigestConfig config, Clock clock) {
        NamespacedCommands.register(
            dispatcher, SeersDigest.MOD_ID, NAME, name -> Commands.literal(name)
                .requires(ADMIN)
                .then(Commands.literal("publish")
                    .executes(CommandExecutors.logged(context -> publish(context, config, clock))))
                .then(Commands.literal("list")
                    .executes(CommandExecutors.logged(context -> list(context, clock))))
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

    // e.g. "#3: 16 Nov to 23 Nov 2026, 12 players (2 new)"
    private static String describe(Issue issue, ZoneId zone) {
        return "<gold>#%d</gold>: %s to %s, %d %s (%d new)".formatted(
            issue.number(),
            DAY.format(issue.started().atZone(zone)),
            DAY_AND_YEAR.format(issue.ended().atZone(zone)),
            issue.activePlayers().size(),
            issue.activePlayers().size() == 1 ? "player" : "players",
            issue.newPlayers().size()
        );
    }
}
