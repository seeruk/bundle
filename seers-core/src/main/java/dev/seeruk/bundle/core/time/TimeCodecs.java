package dev.seeruk.bundle.core.time;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Codecs for times, for saved data and config.
 */
public final class TimeCodecs {
    /**
     * A moment in time, written as ISO-8601 in UTC, e.g. {@code 2026-11-22T18:00:00Z}.
     */
    public static final Codec<Instant> INSTANT = Codec.STRING.comapFlatMap(
        instant -> {
            try {
                return DataResult.success(Instant.parse(instant));
            } catch (DateTimeParseException e) {
                return DataResult.error(() -> "Not a moment in time: " + instant);
            }
        },
        Instant::toString
    );

    /**
     * A time of day, written as {@code HH:MM}, e.g. {@code 18:00}.
     */
    public static final Codec<LocalTime> LOCAL_TIME = Codec.STRING.comapFlatMap(
        time -> {
            try {
                return DataResult.success(LocalTime.parse(time));
            } catch (DateTimeParseException e) {
                return DataResult.error(() -> "Time should be written as HH:MM: " + time);
            }
        },
        LocalTime::toString
    );

    private TimeCodecs() {}
}
