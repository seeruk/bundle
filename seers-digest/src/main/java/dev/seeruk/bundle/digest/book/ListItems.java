package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.book.template.Value;
import dev.seeruk.bundle.digest.book.template.Value.Amount;
import dev.seeruk.bundle.digest.book.template.Value.Text;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The items of the lists a book's sections can show (see {@link ListSource}), each with the values
 * its entry can show.
 */
final class ListItems {
    private ListItems() {}

    /**
     * One item of a list.
     *
     * @param group  The group it's about, for lists of groups' players.
     * @param values Its values, by name.
     */
    record ListItem(Optional<String> group, Map<String, Value> values) {}

    /**
     * The list's items, in its order.
     */
    static List<ListItem> of(ListSource source, Issue issue, DigestConfig config, Function<UUID, Component> names) {
        final var moments = issue.moments();
        return switch (source.kind()) {
            case DEATHS -> moments.deaths().stream().map(death -> {
                final var values = player(death.player(), names);
                values.put("cause", Text.of(death.cause()));
                death.killer().ifPresent(killer -> values.put("killer", Text.of(killer)));
                return item(values);
            }).toList();
            case ADVANCEMENTS -> moments.advancements().stream().map(earned -> {
                final var values = player(earned.player(), names);
                values.put("advancement", Text.of(earned.advancement()));
                return item(values);
            }).toList();
            case FIRSTS -> moments.firsts().stream().map(first -> {
                final var values = player(first.player(), names);
                values.put("what", Text.of(first.what()));
                return item(values);
            }).toList();
            case NEW_PLAYERS -> issue.newPlayers().stream().map(player -> item(player(player, names))).toList();
            case LEADERS -> config.groups().stream()
                .flatMap(group -> leader(group, issue, names).stream())
                .toList();
            case RANKING -> config.groups().stream()
                .filter(group -> source.group().orElseThrow().equals(group.id()))
                .findFirst()
                .map(group -> ranking(group, issue, names))
                .orElse(List.of());
        };
    }

    // The group's leader, if anyone scored in it
    private static Optional<ListItem> leader(DigestConfig.StatGroup group, Issue issue, Function<UUID, Component> names) {
        return Optional.ofNullable(issue.groups().get(group.id()))
            .flatMap(result -> result.players().stream().findFirst())
            .map(leader -> {
                final var values = player(leader.player(), names);
                values.put("group", Text.of(readable(group.id())));
                values.put("value", Amount.of(leader.value(), group.unit()));
                return new ListItem(Optional.of(group.id()), values);
            });
    }

    // The group's players, highest first, with their ranks
    private static List<ListItem> ranking(DigestConfig.StatGroup group, Issue issue, Function<UUID, Component> names) {
        final var players = issue.groups().getOrDefault(group.id(), new Issue.GroupResult(0, List.of())).players();
        return IntStream.range(0, players.size()).mapToObj(index -> {
            final var values = player(players.get(index).player(), names);
            values.put("rank", Amount.count(index + 1));
            values.put("value", Amount.of(players.get(index).value(), group.unit()));
            return new ListItem(Optional.of(group.id()), values);
        }).toList();
    }

    // The player's name, as values that more can be added to
    private static Map<String, Value> player(UUID player, Function<UUID, Component> names) {
        final var values = new HashMap<String, Value>();
        values.put("player", new Text(names.apply(player)));
        return values;
    }

    private static ListItem item(Map<String, Value> values) {
        return new ListItem(Optional.empty(), values);
    }

    // A group's ID as words, e.g. "Night owl" for night_owl
    private static String readable(String id) {
        final var words = id.replace('_', ' ');
        return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
