package dev.seeruk.bundle.core.testing;

import dev.seeruk.bundle.core.time.Schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test utilities for times and schedules, so tests about periods don't depend on the real clock.
 */
public final class TestTimes {
    /**
     * Every day at 18:00, in UTC.
     */
    public static final Schedule DAILY = Schedule.daily(LocalTime.of(18, 0));

    // How many unique days have been handed out. Each is far enough from the next that tests can
    // use the days (and years) after theirs without reaching another test's.
    private static final AtomicLong DAYS = new AtomicLong();
    private static final long DAYS_APART = 10_000;

    private TestTimes() {}

    /**
     * A clock that always says it's the given time, in the time's zone, for code that asks a clock
     * what time it is, e.g. {@code endIfDue(server, config, at(day.plusDays(1)))}.
     */
    public static Clock at(ZonedDateTime time) {
        return Clock.fixed(time.toInstant(), time.getZone());
    }

    /**
     * 19:00 on a day no other test uses, an hour after that day's {@link #DAILY} period started.
     * Snapshots are shared by every period that starts at the same moment, so tests can't share
     * days.
     */
    public static ZonedDateTime uniqueDay() {
        final var day = LocalDate.of(2000, 1, 1).plusDays(DAYS.getAndIncrement() * DAYS_APART);
        return ZonedDateTime.of(day, LocalTime.of(19, 0), ZoneOffset.UTC);
    }
}
