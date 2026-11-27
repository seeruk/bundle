package dev.seeruk.bundle.core.time;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;

/**
 * A moment that comes round again, e.g. every Sunday at 18:00, or the last day of every month at
 * midnight.
 *
 * @param every How often it comes round.
 * @param day   Which day it's on: for {@link Every#WEEK}, a {@link DayOfWeek} number (1 is Monday);
 *              for {@link Every#MONTH}, a day of the month, where a day past the end of a shorter
 *              month means its last day; ignored for {@link Every#DAY}.
 * @param at    The time of day.
 */
public record Schedule(Every every, int day, LocalTime at) {
    private static final int LAST_DAY = 31;

    // Written as it is in config, e.g. "sunday", 1, or "last", until it's checked against `every`
    private record Written(Every every, Optional<Either<Integer, String>> on, LocalTime at) {}

    /**
     * The fields on their own, so they can sit alongside others, e.g. in a period's config.
     */
    public static final MapCodec<Schedule> MAP_CODEC = RecordCodecBuilder.<Written>mapCodec(instance -> instance.group(
        Every.CODEC.fieldOf("every").forGetter(Written::every),
        Codec.either(Codec.INT, Codec.STRING).optionalFieldOf("on").forGetter(Written::on),
        TimeCodecs.LOCAL_TIME.fieldOf("at").forGetter(Written::at)
    ).apply(instance, Written::new)).flatXmap(Schedule::read, schedule -> DataResult.success(schedule.written()));

    public static final Codec<Schedule> CODEC = MAP_CODEC.codec();

    public enum Every implements StringRepresentable {
        DAY("day"),
        WEEK("week"),
        MONTH("month");

        public static final Codec<Every> CODEC = StringRepresentable.fromEnum(Every::values);

        private final String name;

        Every(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static Schedule daily(LocalTime at) {
        return new Schedule(Every.DAY, 0, at);
    }

    public static Schedule weekly(DayOfWeek day, LocalTime at) {
        return new Schedule(Every.WEEK, day.getValue(), at);
    }

    /**
     * @param day A day of the month, from 1 to 31. A day past the end of a shorter month means its
     *            last day, so 31 is always the last day.
     * @param at  The time of day.
     */
    public static Schedule monthly(int day, LocalTime at) {
        return new Schedule(Every.MONTH, day, at);
    }

    /**
     * The most recent moment it came round, at or before the given time, in the time's zone. It
     * works from any time, so it's up to the caller to give it the time they mean, e.g. now (from a
     * clock), or a moment in the past to look back from.
     */
    public Instant latestAtOrBefore(ZonedDateTime time) {
        final var day = time.toLocalDate();
        final var latest = ZonedDateTime.of(scheduledDayOnOrBefore(day), at, time.getZone());

        // The day might be a scheduled day, but before its time, in which case it hadn't come round
        // yet, so it's the scheduled day before it
        return latest.isAfter(time)
            ? ZonedDateTime.of(scheduledDayOnOrBefore(day.minusDays(1)), at, time.getZone()).toInstant()
            : latest.toInstant();
    }

    // The most recent day it comes round on, on or before the date
    private LocalDate scheduledDayOnOrBefore(LocalDate date) {
        return switch (every) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(day)));
            case MONTH -> {
                final var thisMonth = dayInMonth(date);
                yield thisMonth.isAfter(date) ? dayInMonth(date.minusMonths(1)) : thisMonth;
            }
        };
    }

    // The day it comes round on in the date's month, which is the month's last day if it's shorter
    private LocalDate dayInMonth(LocalDate date) {
        return date.withDayOfMonth(Math.min(day, date.lengthOfMonth()));
    }

    private static DataResult<Schedule> read(Written written) {
        final var on = written.on().map(either -> either.map(String::valueOf, value -> value.toLowerCase(Locale.ROOT)));
        return switch (written.every()) {
            case DAY -> on.isPresent()
                ? DataResult.error(() -> "A daily schedule doesn't take 'on'")
                : DataResult.success(daily(written.at()));
            case WEEK -> on.flatMap(Schedule::dayOfWeek)
                .map(day -> DataResult.success(weekly(day, written.at())))
                .orElseGet(() -> DataResult.error(() -> "A weekly schedule needs 'on' to be a day, e.g. sunday"));
            case MONTH -> on.flatMap(Schedule::dayOfMonth)
                .map(day -> DataResult.success(monthly(day, written.at())))
                .orElseGet(() -> DataResult.error(() -> "A monthly schedule needs 'on' to be 1 to 31, or last"));
        };
    }

    private Written written() {
        return new Written(
            every, switch (every) {
            case DAY -> Optional.empty();
            case WEEK -> Optional.of(Either.right(DayOfWeek.of(day).name().toLowerCase(Locale.ROOT)));
            case MONTH -> Optional.of(day == LAST_DAY ? Either.right("last") : Either.left(day));
        }, at
        );
    }

    private static Optional<DayOfWeek> dayOfWeek(String on) {
        try {
            return Optional.of(DayOfWeek.valueOf(on.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> dayOfMonth(String on) {
        if (on.equals("last")) {
            return Optional.of(LAST_DAY);
        }

        try {
            final var day = Integer.parseInt(on);
            return day >= 1 && day <= LAST_DAY ? Optional.of(day) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
