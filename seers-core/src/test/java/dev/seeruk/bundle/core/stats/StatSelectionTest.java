package dev.seeruk.bundle.core.stats;

import com.mojang.serialization.JavaOps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatSelectionTest {
    @Nested
    class Codec {
        @Test
        void roundTrips() {
            final var patterns = List.of("minecraft.mined:*", "!minecraft.mined:minecraft.dirt");
            final var encoded = StatSelection.CODEC.encodeStart(
                JavaOps.INSTANCE,
                StatSelection.of(patterns)
            ).getOrThrow();
            final var decoded = StatSelection.CODEC.parse(JavaOps.INSTANCE, encoded).getOrThrow();

            assertEquals(patterns, decoded.patterns());
        }

        static Stream<Arguments> shouldRefuseEmptyPatterns() {
            return Stream.of(
                Arguments.of("Refuses an empty pattern", List.of("minecraft.mined:*", "")),
                Arguments.of("Refuses a blank pattern", List.of(" ")),
                Arguments.of("Refuses a lone !", List.of("!"))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRefuseEmptyPatterns(final String description, final List<String> patterns) {
            assertTrue(StatSelection.CODEC.parse(JavaOps.INSTANCE, patterns).isError());
            assertThrows(IllegalArgumentException.class, () -> StatSelection.of(patterns));
        }
    }

    @Nested
    class Matches {
        static Stream<Arguments> shouldMatch() {
            return Stream.of(
                Arguments.of(
                    "Matches an exact name",
                    List.of("minecraft.mined:minecraft.stone"),
                    "minecraft.mined:minecraft.stone",
                    true
                ),
                Arguments.of(
                    "Doesn't match another name",
                    List.of("minecraft.mined:minecraft.stone"),
                    "minecraft.mined:minecraft.dirt",
                    false
                ),
                Arguments.of(
                    "Matches a whole name, not part of one",
                    List.of("minecraft.mined:minecraft.stone"),
                    "minecraft.mined:minecraft.stone_bricks",
                    false
                ),
                Arguments.of(
                    "Matches everything after a *",
                    List.of("minecraft.mined:*"),
                    "minecraft.mined:minecraft.dirt",
                    true
                ),
                Arguments.of(
                    "Matches a * in the middle",
                    List.of("minecraft.custom:minecraft.*_one_cm"),
                    "minecraft.custom:minecraft.walk_one_cm",
                    true
                ),
                Arguments.of(
                    "Doesn't treat a . as any character",
                    List.of("minecraft.mined:*"),
                    "minecraftXmined:minecraft.dirt",
                    false
                ),
                Arguments.of(
                    "Matches any of several patterns",
                    List.of("minecraft.mined:*", "minecraft.killed:*"),
                    "minecraft.killed:minecraft.zombie",
                    true
                ),
                Arguments.of(
                    "Matches a scoreboard objective",
                    List.of("scoreboard:seers_*.blocks_mined"),
                    "scoreboard:seers_resource_zones.blocks_mined",
                    true
                ),
                Arguments.of(
                    "Excludes with a !",
                    List.of("minecraft.custom:*", "!minecraft.custom:minecraft.fall_one_cm"),
                    "minecraft.custom:minecraft.fall_one_cm",
                    false
                ),
                Arguments.of(
                    "Excludes regardless of the order the patterns are in",
                    List.of("!minecraft.custom:minecraft.fall_one_cm", "minecraft.custom:*"),
                    "minecraft.custom:minecraft.fall_one_cm",
                    false
                ),
                Arguments.of(
                    "Keeps what an exclusion doesn't match",
                    List.of("minecraft.custom:*", "!minecraft.custom:minecraft.fall_one_cm"),
                    "minecraft.custom:minecraft.walk_one_cm",
                    true
                ),
                Arguments.of(
                    "Matches nothing with only exclusions",
                    List.of("!minecraft.custom:minecraft.fall_one_cm"),
                    "minecraft.custom:minecraft.walk_one_cm",
                    false
                ),
                Arguments.of(
                    "Matches nothing with no patterns",
                    List.of(),
                    "minecraft.custom:minecraft.walk_one_cm",
                    false
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldMatch(final String description, final List<String> patterns, final String name, final boolean expected) {
            assertEquals(expected, StatSelection.of(patterns).matches(name));
        }
    }

    @Nested
    class Sum {
        private static final Map<String, Integer> VALUES = Map.of(
            "minecraft.mined:minecraft.stone", 10,
            "minecraft.mined:minecraft.dirt", 5,
            "minecraft.killed:minecraft.zombie", 3
        );

        static Stream<Arguments> shouldSum() {
            return Stream.of(
                Arguments.of("Adds up every selected stat", List.of("minecraft.mined:*"), 15L),
                Arguments.of(
                    "Counts a stat once when patterns overlap",
                    List.of("minecraft.mined:*", "minecraft.mined:minecraft.stone"),
                    15L
                ),
                Arguments.of(
                    "Leaves out excluded stats",
                    List.of("minecraft.mined:*", "!minecraft.mined:minecraft.dirt"),
                    10L
                ),
                Arguments.of(
                    "Adds stats of different types",
                    List.of("minecraft.mined:minecraft.dirt", "minecraft.killed:*"),
                    8L
                ),
                Arguments.of("Is zero when nothing matches", List.of("scoreboard:missing"), 0L)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldSum(final String description, final List<String> patterns, final long expected) {
            assertEquals(expected, StatSelection.of(patterns).sum(VALUES));
        }

        @Test
        void addsBeyondIntegerLimits() {
            final var values = Map.of("a", Integer.MAX_VALUE, "b", Integer.MAX_VALUE);
            assertEquals(2L * Integer.MAX_VALUE, StatSelection.of(List.of("*")).sum(values));
        }
    }

    @Nested
    class ExactNames {
        @Test
        void listsPatternsWithoutWildcards() {
            final var selection = StatSelection.of(List.of(
                "minecraft.mined:*",
                "minecraft.custom:minecraft.fish_caught",
                "!minecraft.custom:minecraft.fall_one_cm"
            ));

            assertEquals(
                List.of("minecraft.custom:minecraft.fish_caught", "minecraft.custom:minecraft.fall_one_cm"),
                selection.exactNames()
            );
        }
    }
}
