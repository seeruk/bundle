package dev.seeruk.bundle.digest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NightHoursTest {
    private static final NightHours EARLY = new NightHours(LocalTime.MIDNIGHT, LocalTime.of(6, 0));
    private static final NightHours LATE = new NightHours(LocalTime.of(22, 0), LocalTime.of(4, 0));

    static Stream<Arguments> shouldContain() {
        return Stream.of(
            Arguments.of("Contains a time in the hours", EARLY, "03:00", true),
            Arguments.of("Contains when they start", EARLY, "00:00", true),
            Arguments.of("Doesn't contain when they end", EARLY, "06:00", false),
            Arguments.of("Doesn't contain a time outside them", EARLY, "12:00", false),
            Arguments.of("Contains a time before midnight, past midnight", LATE, "23:00", true),
            Arguments.of("Contains a time after midnight, past midnight", LATE, "01:00", true),
            Arguments.of("Contains when they start, past midnight", LATE, "22:00", true),
            Arguments.of("Doesn't contain when they end, past midnight", LATE, "04:00", false),
            Arguments.of("Doesn't contain a time outside them, past midnight", LATE, "12:00", false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldContain(final String description, final NightHours hours, final String time, final boolean expected) {
        assertEquals(expected, hours.contains(LocalTime.parse(time)));
    }
}
