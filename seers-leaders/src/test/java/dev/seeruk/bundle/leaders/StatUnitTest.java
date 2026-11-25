package dev.seeruk.bundle.leaders;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatUnitTest {
    static Stream<Arguments> shouldFormat() {
        return Stream.of(
            Arguments.of("Shows a count as it is", StatUnit.COUNT, 42L, "42"),
            Arguments.of("Separates thousands", StatUnit.COUNT, 1_234_567L, "1,234,567"),
            Arguments.of("Shows distance in blocks", StatUnit.DISTANCE, 128_400L, "1,284"),
            Arguments.of("Rounds distance down to whole blocks", StatUnit.DISTANCE, 199L, "1"),
            Arguments.of("Shows time in hours", StatUnit.TIME, 72_000L, "1.0"),
            Arguments.of("Shows time to one decimal place", StatUnit.TIME, 252_000L, "3.5"),
            Arguments.of("Separates thousands of hours", StatUnit.TIME, 72_000_000L, "1,000.0")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldFormat(final String description, final StatUnit unit, final long value, final String expected) {
        assertEquals(expected, unit.format(value));
    }
}
