package dev.seeruk.bundle.digest.book.template;

import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.digest.book.template.Value.Amount;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FiltersTest {
    // An hour is 72,000 ticks, and a block is 100 centimetres
    private static final Variables VARIABLES = new Variables(Map.ofEntries(
        Map.entry("one", Amount.count(1)),
        Map.entry("three", Amount.count(3)),
        Map.entry("big", Amount.count(1_234_567)),
        Map.entry("zero", Amount.count(0)),
        Map.entry("distance", Amount.of(250_000, StatUnit.DISTANCE)),
        Map.entry("hour", Amount.of(72_000, StatUnit.TIME)),
        Map.entry("long", Amount.of(72_000 * 30, StatUnit.TIME)),
        Map.entry("longer", Amount.of(72_000 * 30 + 36_000, StatUnit.TIME)),
        Map.entry("almost", Amount.of(72_000 + 2_000, StatUnit.TIME)),
        Map.entry("moment", Amount.of(2_000, StatUnit.TIME)),
        Map.entry("name", Value.Text.of("Seer"))
    ));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> shouldFill() {
        return Stream.of(
            Arguments.of("Shows counts as they are", "big", Optional.of("1,234,567")),
            Arguments.of("Shows distances in blocks", "distance", Optional.of("2,500")),
            Arguments.of("Shows whole hours without decimals", "long", Optional.of("30")),
            Arguments.of("Shows times in hours to 1 place when it has one", "longer", Optional.of("30.5")),
            Arguments.of("Leaves zeroes off the end of decimals", "long|days|round:3", Optional.of("1.25")),
            Arguments.of("Shows text as it is", "name", Optional.of("Seer")),
            Arguments.of("Pluralises numbers other than 1", "three|plural:player", Optional.of("3 players")),
            Arguments.of("Doesn't pluralise 1", "one|plural:player", Optional.of("1 player")),
            Arguments.of("Pluralises 0", "zero|plural:player", Optional.of("0 players")),
            Arguments.of("Pluralises irregular words", "three|plural:person:people", Optional.of("3 people")),
            Arguments.of("Pluralises by the number as it's shown", "almost|plural:hour", Optional.of("1 hour")),
            Arguments.of("Shows distances in kilometres", "distance|km", Optional.of("2.5")),
            Arguments.of("Shows times in minutes", "hour|minutes", Optional.of("60")),
            Arguments.of("Shows times in days", "long|days", Optional.of("1.3")),
            Arguments.of("Rounds to the given places", "long|days|round:2", Optional.of("1.25")),
            Arguments.of("Applies filters in order", "hour|minutes|plural:minute", Optional.of("60 minutes")),
            Arguments.of("Shows nothing for numbers shown as 0", "moment", Optional.empty()),
            Arguments.of("Shows nothing for plurals of numbers shown as 0", "moment|plural:hour", Optional.empty()),
            Arguments.of("Shows real zeroes", "zero", Optional.of("0")),
            Arguments.of("Shows nothing for units that don't fit", "distance|hours", Optional.empty()),
            Arguments.of("Shows nothing for filters on text", "name|plural:name", Optional.empty()),
            Arguments.of("Shows nothing for unknown filters", "three|loudly", Optional.empty()),
            Arguments.of("Shows nothing for plurals without a word", "three|plural", Optional.empty()),
            Arguments.of("Shows nothing for unknown values", "nobody", Optional.empty())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldFill(final String description, final String placeholder, final Optional<String> expected) {
        assertEquals(expected, VARIABLES.fill(placeholder).map(Component::getString));
    }
}
