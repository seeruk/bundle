package dev.seeruk.bundle.leaders;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ranks players by their totals in a category.
 */
public final class Leaderboard {
    private Leaderboard() {}

    /**
     * Who is on a leaderboard, and their total. Their rank is where they are in the ranked list, so
     * it isn't kept here. Compare with {@link Seat}, which is the place itself, whoever holds it.
     *
     * @param player Who holds the rank.
     * @param value  Their total.
     */
    public record Standing(UUID player, long value) {}

    /**
     * Ranks players from highest total to lowest, leaving out anyone on zero. Players on the same
     * total keep their order from {@code previous}, so a tie never takes a rank from its holder.
     * Anyone else tied is ordered by UUID, so ranks don't change between refreshes.
     *
     * @param totals   Each player's total.
     * @param previous Players in the order they were last ranked.
     */
    public static List<Standing> rank(Map<UUID, Long> totals, List<UUID> previous) {
        return totals.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .map(entry -> new Standing(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingLong(Standing::value).reversed()
                .thenComparingInt(standing -> previousIndex(previous, standing.player()))
                .thenComparing(Standing::player))
            .toList();
    }

    private static int previousIndex(List<UUID> previous, UUID player) {
        final var index = previous.indexOf(player);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}
