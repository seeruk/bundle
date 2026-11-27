package dev.seeruk.bundle.core.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utilities for comparing players' stats over time, e.g. with a {@link StatSnapshots snapshot}.
 * Stats are shaped like {@link AllPlayerStats#read}, so any result can be summed by a
 * {@link StatSelection} the same way.
 */
public final class StatChanges {
    private StatChanges() {}

    /**
     * Returns how much each stat in {@code current} has gone up since {@code before}. Stats that
     * didn't go up are left out, so a value that went down (e.g. a reset scoreboard objective)
     * counts as nothing, rather than taking away. Stats missing from {@code before} count from 0.
     */
    public static Map<UUID, Map<String, Integer>> since(
        Map<UUID, Map<String, Integer>> current,
        Map<UUID, Map<String, Integer>> before
    ) {
        final var changes = new HashMap<UUID, Map<String, Integer>>();
        current.forEach((player, values) -> {
            final var was = before.getOrDefault(player, Map.of());
            final var changed = new HashMap<String, Integer>();
            values.forEach((name, value) -> {
                final var change = value - was.getOrDefault(name, 0);
                if (change > 0) {
                    changed.put(name, change);
                }
            });
            changes.put(player, changed);
        });
        return changes;
    }
}
