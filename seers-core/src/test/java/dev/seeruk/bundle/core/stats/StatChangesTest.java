package dev.seeruk.bundle.core.stats;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatChangesTest {
    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void countsHowMuchStatsWentUp() {
        final var changes = StatChanges.since(
            Map.of(PLAYER, Map.of("jumps", 10, "mined", 4)),
            Map.of(PLAYER, Map.of("jumps", 7, "mined", 4))
        );

        assertEquals(Map.of(PLAYER, Map.of("jumps", 3)), changes);
    }

    @Test
    void countsNewStatsFromZero() {
        final var changes = StatChanges.since(
            Map.of(PLAYER, Map.of("jumps", 10)),
            Map.of(PLAYER, Map.of())
        );

        assertEquals(Map.of(PLAYER, Map.of("jumps", 10)), changes);
    }

    @Test
    void countsNewPlayersFromZero() {
        final var changes = StatChanges.since(Map.of(PLAYER, Map.of("jumps", 10)), Map.of());

        assertEquals(Map.of(PLAYER, Map.of("jumps", 10)), changes);
    }

    @Test
    void leavesOutStatsThatWentDown() {
        final var changes = StatChanges.since(
            Map.of(PLAYER, Map.of("scoreboard:reset", 2)),
            Map.of(PLAYER, Map.of("scoreboard:reset", 50))
        );

        assertEquals(Map.of(PLAYER, Map.of()), changes);
    }
}
