package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.digest.DigestConfig.StatGroup;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.api.Issue.GroupResult;
import dev.seeruk.bundle.digest.api.Issue.PlayerTotal;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Utilities for making issues from what players have done since the last one.
 */
public final class Issues {
    // Highest first, then by UUID, so players on the same total are always in the same order
    private static final Comparator<PlayerTotal> RANKING = Comparator.comparingLong(PlayerTotal::value).reversed()
        .thenComparing(PlayerTotal::player);

    private Issues() {}

    /**
     * Makes an issue from how much each player's stats went up in the time it covers, or nothing if
     * nobody's did. Every group is included, even one nobody scored in.
     *
     * @param number         The issue's number.
     * @param started        When the time it covers started.
     * @param ended          When the time it covers ended.
     * @param groups         The stat groups to add up.
     * @param changes        How much each player's stats went up in that time (see
     *                       {@link dev.seeruk.bundle.core.stats.StatChanges}).
     * @param playersAtStart Who had stats when that time started.
     * @param moments        What happened in that time that isn't a stat.
     */
    public static Optional<Issue> build(
        int number,
        Instant started,
        Instant ended,
        List<StatGroup> groups,
        Map<UUID, Map<String, Integer>> changes,
        Set<UUID> playersAtStart,
        Issue.Moments moments
    ) {
        // During the period for this issue, anyone with any stat that went up, or that recorded any
        // moment, was active. Changes only include stats that went up, so an empty map means they
        // did nothing. The result is sorted so the list is the same every time.
        final var active = Stream.concat(
                changes.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).map(Map.Entry::getKey),
                moments.players().stream()
            )
            .distinct()
            .sorted()
            .toList();
        if (active.isEmpty()) {
            return Optional.empty(); // Nothing happened, so there's nothing to publish
        }

        final var results = new HashMap<String, GroupResult>();
        for (final var group : groups) {
            // Each player's total for the group's stats, highest first, leaving out anyone on zero
            final var players = changes.entrySet().stream()
                .map(entry -> new PlayerTotal(entry.getKey(), group.stats().sum(entry.getValue())))
                .filter(total -> total.value() > 0)
                .sorted(RANKING)
                .toList();
            results.put(group.id(), new GroupResult(players.stream().mapToLong(PlayerTotal::value).sum(), players));
        }

        // New players were active, but had no stats at all when the issue's time started
        final var newPlayers = active.stream().filter(player -> !playersAtStart.contains(player)).toList();
        return Optional.of(new Issue(number, started, ended, Map.copyOf(results), active, newPlayers, moments));
    }
}
