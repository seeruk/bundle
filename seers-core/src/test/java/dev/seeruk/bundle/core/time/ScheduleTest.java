package dev.seeruk.bundle.core.time;

import com.mojang.serialization.JavaOps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ScheduleTest {
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);

    @Nested
    class Latest {
        // 2026-11-25 is a Wednesday, in GMT
        static Stream<Arguments> shouldFindTheLatestMoment() {
            return Stream.of(
                Arguments.of("Daily, later that day", Schedule.daily(SIX_PM), "2026-11-25T19:00", "2026-11-25T18:00:00Z"),
                Arguments.of("Daily, earlier that day", Schedule.daily(SIX_PM), "2026-11-25T17:00", "2026-11-24T18:00:00Z"),
                Arguments.of("Daily, exactly then", Schedule.daily(SIX_PM), "2026-11-25T18:00", "2026-11-25T18:00:00Z"),
                Arguments.of(
                    "Weekly, later in the week",
                    Schedule.weekly(DayOfWeek.SUNDAY, SIX_PM),
                    "2026-11-25T12:00",
                    "2026-11-22T18:00:00Z"
                ),
                Arguments.of(
                    "Weekly, earlier that day",
                    Schedule.weekly(DayOfWeek.SUNDAY, SIX_PM),
                    "2026-11-22T17:00",
                    "2026-11-15T18:00:00Z"
                ),
                Arguments.of(
                    "Weekly, across the end of a month",
                    Schedule.weekly(DayOfWeek.WEDNESDAY, SIX_PM),
                    "2026-12-01T12:00",
                    "2026-11-25T18:00:00Z"
                ),
                Arguments.of("Monthly, later in the month", Schedule.monthly(1, LocalTime.MIDNIGHT), "2026-11-25T12:00", "2026-11-01T00:00:00Z"),
                Arguments.of(
                    "Monthly, on a day past the end of a shorter month",
                    Schedule.monthly(31, SIX_PM),
                    "2026-03-01T12:00",
                    "2026-02-28T18:00:00Z"
                ),
                Arguments.of("Monthly, on a shorter month's last day", Schedule.monthly(31, SIX_PM), "2026-02-28T19:00", "2026-02-28T18:00:00Z"),
                Arguments.of(
                    "In summer time, in local time",
                    Schedule.weekly(DayOfWeek.SUNDAY, SIX_PM),
                    "2026-03-29T19:00",
                    "2026-03-29T17:00:00Z"
                ),
                Arguments.of(
                    "At a time skipped when the clocks go forward, an hour later",
                    Schedule.daily(LocalTime.of(1, 30)),
                    "2026-03-29T12:00",
                    "2026-03-29T01:30:00Z"
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldFindTheLatestMoment(final String description, final Schedule schedule, final String now, final String expected) {
            final var at = ZonedDateTime.of(LocalDateTime.parse(now), LONDON);
            assertEquals(Instant.parse(expected), schedule.latest(at));
        }
    }

    @Nested
    class Codec {
        static Stream<Arguments> shouldRead() {
            return Stream.of(
                Arguments.of("Daily", Map.of("every", "day", "at", "18:00"), Schedule.daily(SIX_PM)),
                Arguments.of("Weekly", Map.of("every", "week", "on", "sunday", "at", "18:00"), Schedule.weekly(DayOfWeek.SUNDAY, SIX_PM)),
                Arguments.of("Monthly, on a day", Map.of("every", "month", "on", 1, "at", "18:00"), Schedule.monthly(1, SIX_PM)),
                Arguments.of("Monthly, on the last day", Map.of("every", "month", "on", "last", "at", "18:00"), Schedule.monthly(31, SIX_PM))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRead(final String description, final Map<String, Object> written, final Schedule expected) {
            final var schedule = Schedule.CODEC.parse(JavaOps.INSTANCE, written).getOrThrow();
            assertEquals(expected, schedule);

            final var encoded = Schedule.CODEC.encodeStart(JavaOps.INSTANCE, schedule).getOrThrow();
            assertEquals(expected, Schedule.CODEC.parse(JavaOps.INSTANCE, encoded).getOrThrow());
        }

        static Stream<Arguments> shouldRefuse() {
            return Stream.of(
                Arguments.of("A daily schedule with a day", Map.of("every", "day", "on", "sunday", "at", "18:00")),
                Arguments.of("A weekly schedule without a day", Map.of("every", "week", "at", "18:00")),
                Arguments.of("A weekly schedule on a number", Map.of("every", "week", "on", 1, "at", "18:00")),
                Arguments.of("A monthly schedule on a day name", Map.of("every", "month", "on", "sunday", "at", "18:00")),
                Arguments.of("A monthly schedule past the 31st", Map.of("every", "month", "on", 32, "at", "18:00")),
                Arguments.of("A monthly schedule before the 1st", Map.of("every", "month", "on", 0, "at", "18:00")),
                Arguments.of("A time that isn't HH:MM", Map.of("every", "day", "at", "6pm"))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRefuse(final String description, final Map<String, Object> written) {
            assertTrue(Schedule.CODEC.parse(JavaOps.INSTANCE, written).isError());
        }
    }
}
