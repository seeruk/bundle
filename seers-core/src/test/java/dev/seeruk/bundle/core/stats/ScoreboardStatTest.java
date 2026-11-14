package dev.seeruk.bundle.core.stats;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoreboardStatTest {
    static Stream<Arguments> shouldBuildObjectiveId() {
        return Stream.of(
            Arguments.of("Joins the mod ID and name with a dot", "seers", "items_disposed", "seers.items_disposed"),
            Arguments.of("Swaps hyphens in the mod ID for underscores", "seers-item-text", "name_written", "seers_item_text.name_written")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldBuildObjectiveId(final String description, final String modId, final String name, final String expected) {
        assertEquals(expected, new ScoreboardStat(modId, name, "Display Name").objectiveId());
    }
}
