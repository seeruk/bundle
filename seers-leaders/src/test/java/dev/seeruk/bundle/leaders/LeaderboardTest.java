package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.leaders.Leaderboard.Standing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeaderboardTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    static Stream<Arguments> shouldRank() {
        return Stream.of(
            Arguments.of("Ranks the highest total first", Map.of(A, 1L, B, 3L, C, 2L), List.of(), List.of(B, C, A)),
            Arguments.of("Leaves out players on zero", Map.of(A, 0L, B, 3L), List.of(), List.of(B)),
            Arguments.of("Is empty when nobody has a total", Map.of(A, 0L), List.of(), List.of()),
            Arguments.of("Keeps the previous order for a tie", Map.of(A, 5L, B, 5L), List.of(B, A), List.of(B, A)),
            Arguments.of(
                "Puts previously ranked players ahead in a tie",
                Map.of(A, 5L, B, 5L),
                List.of(B),
                List.of(B, A)
            ),
            Arguments.of("Orders anyone else tied by UUID", Map.of(B, 5L, A, 5L), List.of(), List.of(A, B)),
            Arguments.of(
                "Ranks a higher total above the previous order",
                Map.of(A, 6L, B, 5L),
                List.of(B, A),
                List.of(A, B)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldRank(final String description, final Map<UUID, Long> totals, final List<UUID> previous, final List<UUID> expected) {
        assertEquals(expected, Leaderboard.rank(totals, previous).stream().map(Standing::player).toList());
    }

    @Test
    void keepsEachPlayersTotal() {
        assertEquals(
            List.of(new Standing(B, 3L), new Standing(A, 1L)),
            Leaderboard.rank(Map.of(A, 1L, B, 3L), List.of())
        );
    }
}
