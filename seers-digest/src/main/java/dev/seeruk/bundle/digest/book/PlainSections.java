package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.api.Issue;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An issue's book sections with fixed wording, until the book is written from templates. Sections
 * with nothing to say are left out.
 */
public final class PlainSections {
    private PlainSections() {}

    /**
     * @param issue  The issue the sections are about.
     * @param config Says each group's unit, and the order groups are shown in.
     * @param names  Gives each player's name.
     */
    public static List<BookSection> of(Issue issue, DigestConfig config, Function<UUID, Component> names) {
        final var headlines = new ArrayList<Component>();
        final var awards = new ArrayList<Component>();
        headlines.add(Component.literal(issue.activePlayers().size() + " active players"));
        headlines.add(Component.literal(issue.newPlayers().size() + " new players"));

        for (final var group : config.groups()) {
            final var result = issue.groups().get(group.id());
            if (result == null || result.players().isEmpty()) {
                continue;
            }

            headlines.add(Component.literal(group.id() + ": " + amount(result.total(), group.unit())));
            final var leader = result.players().getFirst();
            awards.add(Component.literal(group.id() + "\n")
                .append(names.apply(leader.player()))
                .append(": " + amount(leader.value(), group.unit())));
        }

        final var moments = issue.moments();
        return Stream.of(
                section("Headlines", headlines),
                section("Awards", awards),
                section("Deaths", entries(moments.deaths(), death -> about(
                    names.apply(death.player()),
                    " died to " + death.cause() + death.killer().map(killer -> ", killed by " + killer).orElse("")
                ))),
                section("Advancements", entries(moments.advancements(), earned -> about(
                    names.apply(earned.player()),
                    " earned " + earned.advancement()
                ))),
                section("Firsts", entries(moments.firsts(), first -> about(
                    names.apply(first.player()),
                    " was first: " + first.what()
                ))),
                section("Welcome", entries(issue.newPlayers(), player -> Component.literal("Welcome, ")
                    .append(names.apply(player))
                    .append("!")))
            )
            .flatMap(Optional::stream)
            .toList();
    }

    private static <T> List<Component> entries(List<T> items, Function<T, Component> entry) {
        return items.stream().map(entry).toList();
    }

    // e.g. "Seer earned minecraft:story/mine_stone"
    private static Component about(Component name, String rest) {
        return Component.empty().append(name).append(rest);
    }

    private static Optional<BookSection> section(String title, List<Component> entries) {
        return entries.isEmpty() ? Optional.empty() : Optional.of(new BookSection(Component.literal(title), entries));
    }

    // e.g. "1,204 blocks"
    private static String amount(long value, StatUnit unit) {
        return unit.format(value) + switch (unit) {
            case COUNT -> "";
            case DISTANCE -> " blocks";
            case TIME -> " hours";
        };
    }
}
