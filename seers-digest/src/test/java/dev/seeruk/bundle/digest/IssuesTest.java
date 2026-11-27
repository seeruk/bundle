package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.digest.DigestConfig.StatGroup;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.api.Issue.GroupResult;
import dev.seeruk.bundle.digest.api.Issue.PlayerTotal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssuesTest {
    private static final Instant STARTED = Instant.parse("2026-11-15T18:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-11-22T18:00:00Z");

    // In UUID order, so ties are easy to read
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private static final List<StatGroup> GROUPS = List.of(
        new StatGroup("miner", StatSelection.of(List.of("mined:*")), StatUnit.COUNT),
        new StatGroup("angler", StatSelection.of(List.of("fish")), StatUnit.COUNT)
    );

    private static Optional<Issue> build(Map<UUID, Map<String, Integer>> changes, Set<UUID> playersAtStart) {
        return build(changes, playersAtStart, Issue.Moments.EMPTY);
    }

    private static Optional<Issue> build(
        Map<UUID, Map<String, Integer>> changes,
        Set<UUID> playersAtStart,
        Issue.Moments moments
    ) {
        return Issues.build(7, STARTED, ENDED, GROUPS, changes, playersAtStart, moments);
    }

    @Test
    void addsUpAndRanksEachGroup() {
        final var issue = build(
            Map.of(
                FIRST, Map.of("mined:stone", 3, "mined:dirt", 2),
                SECOND, Map.of("mined:stone", 10)
            ),
            Set.of(FIRST, SECOND)
        ).orElseThrow();

        assertEquals(
            new GroupResult(15, List.of(new PlayerTotal(SECOND, 10), new PlayerTotal(FIRST, 5))),
            issue.groups().get("miner")
        );
    }

    @Test
    void ranksTiesByPlayer() {
        final var issue = build(
            Map.of(THIRD, Map.of("fish", 4), FIRST, Map.of("fish", 4)),
            Set.of(FIRST, THIRD)
        ).orElseThrow();

        assertEquals(
            List.of(new PlayerTotal(FIRST, 4), new PlayerTotal(THIRD, 4)),
            issue.groups().get("angler").players()
        );
    }

    @Test
    void keepsGroupsNobodyScoredIn() {
        final var issue = build(Map.of(FIRST, Map.of("mined:stone", 1)), Set.of(FIRST)).orElseThrow();

        assertEquals(new GroupResult(0, List.of()), issue.groups().get("angler"));
    }

    @Test
    void findsActiveAndNewPlayers() {
        final var issue = build(
            Map.of(
                FIRST, Map.of("jumps", 1),
                SECOND, Map.of(),
                THIRD, Map.of("jumps", 1)
            ),
            Set.of(FIRST, SECOND)
        ).orElseThrow();

        // Second's stats didn't change, so they weren't active. Third had none at the start.
        assertEquals(List.of(FIRST, THIRD), issue.activePlayers());
        assertEquals(List.of(THIRD), issue.newPlayers());
    }

    @Test
    void keepsTheTimeItCoversAndNumber() {
        final var issue = build(Map.of(FIRST, Map.of("jumps", 1)), Set.of(FIRST)).orElseThrow();

        assertEquals(7, issue.number());
        assertEquals(STARTED, issue.started());
        assertEquals(ENDED, issue.ended());
    }

    @Test
    void makesNoIssueWhenNobodyWasActive() {
        assertEquals(Optional.empty(), build(Map.of(FIRST, Map.of()), Set.of(FIRST)));
    }

    @Test
    void keepsTheMoments() {
        final var moments = new Issue.Moments(
            List.of(new Issue.Death(FIRST, "minecraft:fall", Optional.empty(), ENDED)),
            List.of(),
            List.of(new Issue.First(FIRST, "death:minecraft:fall", ENDED))
        );

        assertEquals(moments, build(Map.of(FIRST, Map.of("jumps", 1)), Set.of(FIRST), moments).orElseThrow().moments());
    }

    @Test
    void makesAnIssueFromAMomentAlone() {
        // Nobody's stats went up, but Second earned an advancement, so they were active
        final var moments = new Issue.Moments(
            List.of(),
            List.of(new Issue.Earned(SECOND, "minecraft:story/root", ENDED)),
            List.of()
        );

        final var issue = build(Map.of(FIRST, Map.of()), Set.of(FIRST, SECOND), moments).orElseThrow();

        assertEquals(List.of(SECOND), issue.activePlayers());
    }
}
