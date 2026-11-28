package dev.seeruk.bundle.digest.book;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The days an issue covers, written as briefly as reads clearly, e.g. "16 to 23 Nov 2026".
 */
public final class IssueDates {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d", Locale.ROOT);
    private static final DateTimeFormatter DAY_AND_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ROOT);
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ROOT);

    private IssueDates() {}

    /**
     * The days from when the issue started to just before it was published, so one published at
     * midnight ends the day before.
     */
    public static String between(Instant started, Instant ended, ZoneId zone) {
        final var from = started.atZone(zone).toLocalDate();
        final var lastMoment = ended.minusNanos(1).atZone(zone).toLocalDate();
        final var to = lastMoment.isBefore(from) ? from : lastMoment;

        if (from.equals(to)) {
            return FULL.format(to);
        }
        if (from.getYear() != to.getYear()) {
            return FULL.format(from) + " to " + FULL.format(to);
        }
        if (from.getMonth() != to.getMonth()) {
            return DAY_AND_MONTH.format(from) + " to " + FULL.format(to);
        }
        return DAY.format(from) + " to " + FULL.format(to);
    }
}
