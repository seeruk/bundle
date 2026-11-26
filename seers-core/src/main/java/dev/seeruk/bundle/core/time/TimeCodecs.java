package dev.seeruk.bundle.core.time;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.time.Instant;
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

    private TimeCodecs() {}
}
