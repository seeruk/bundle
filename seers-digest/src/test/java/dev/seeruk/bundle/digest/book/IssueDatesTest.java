package dev.seeruk.bundle.digest.book;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssueDatesTest {
    static Stream<Arguments> shouldWriteTheDates() {
        return Stream.of(
            Arguments.of("Writes one day once", "2026-11-23T10:00:00Z", "2026-11-23T18:00:00Z", "23 Nov 2026"),
            Arguments.of("Writes the month once within a month", "2026-11-16T18:00:00Z", "2026-11-23T18:00:00Z", "16 to 23 Nov 2026"),
            Arguments.of("Writes both months across months", "2026-11-28T18:00:00Z", "2026-12-03T18:00:00Z", "28 Nov to 3 Dec 2026"),
            Arguments.of("Writes both years across years", "2026-12-28T18:00:00Z", "2027-01-03T18:00:00Z", "28 Dec 2026 to 3 Jan 2027"),
            Arguments.of("Ends the day before when published at midnight", "2026-12-01T00:00:00Z", "2026-12-08T00:00:00Z", "1 to 7 Dec 2026"),
            Arguments.of("Ends no earlier than it started", "2026-12-01T00:00:00Z", "2026-12-01T00:00:00Z", "1 Dec 2026")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldWriteTheDates(final String description, final String started, final String ended, final String expected) {
        assertEquals(expected, IssueDates.between(Instant.parse(started), Instant.parse(ended), ZoneOffset.UTC));
    }
}
