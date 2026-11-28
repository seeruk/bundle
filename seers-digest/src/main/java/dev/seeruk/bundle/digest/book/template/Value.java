package dev.seeruk.bundle.digest.book.template;

import dev.seeruk.bundle.core.stats.StatUnit;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Something a book's templates can show, e.g. a player's name or a stat's total.
 */
public sealed interface Value {
    /**
     * Shown as it is.
     */
    record Text(Component text) implements Value {
        public static Text of(String text) {
            return new Text(Component.literal(text));
        }
    }

    /**
     * A number, shown in a unit (e.g. a distance in blocks) to some decimal places, with thousands
     * separators.
     *
     * @param raw      As it's counted, e.g. centimetres for a distance.
     * @param unit     What kind of stat it is.
     * @param display  The unit it's shown in.
     * @param decimals How many decimal places it's shown to.
     */
    record Amount(long raw, StatUnit unit, Display display, int decimals) implements Value {
        /**
         * The number shown in the unit's usual way: a count as it is, a distance in blocks, and a
         * time in hours to one decimal place.
         */
        public static Amount of(long raw, StatUnit unit) {
            final var display = switch (unit) {
                case COUNT -> Display.AS_IS;
                case DISTANCE -> Display.BLOCKS;
                case TIME -> Display.HOURS;
            };
            return new Amount(raw, unit, display, display.decimals);
        }

        public static Amount count(long count) {
            return of(count, StatUnit.COUNT);
        }

        /**
         * The number in the unit it's shown in.
         */
        public double value() {
            return raw / display.per;
        }

        /**
         * The number in the unit it's shown in, rounded to the places it's shown to.
         */
        public double rounded() {
            final var scale = Math.pow(10, decimals);
            return Math.round(value() * scale) / scale;
        }

        /**
         * The number as it's shown, e.g. {@code 1,204.5}, without zeroes at the end of its decimals,
         * so 142.0 hours reads as 142 hours.
         */
        public String shown() {
            final var shown = String.format(Locale.ROOT, "%,." + decimals + "f", value());
            return shown.contains(".") ? shown.replaceAll("\\.?0+$", "") : shown;
        }

        /**
         * Whether it reads well, which it doesn't if it's more than 0 but shown as 0.
         */
        public boolean readsWell() {
            return raw <= 0 || rounded() != 0;
        }
    }

    /**
     * A unit a number can be shown in, and the units of stat it can show.
     */
    enum Display {
        AS_IS(StatUnit.COUNT, 1, 0),
        BLOCKS(StatUnit.DISTANCE, 100, 0),
        KM(StatUnit.DISTANCE, 100_000, 1),
        MINUTES(StatUnit.TIME, SharedConstants.TICKS_PER_SECOND * 60, 0),
        HOURS(StatUnit.TIME, SharedConstants.TICKS_PER_SECOND * 60 * 60, 1),
        DAYS(StatUnit.TIME, SharedConstants.TICKS_PER_SECOND * 60 * 60 * 24, 1);

        final StatUnit unit;
        // How many of the stat's raw units make one of these
        final double per;
        // How many decimal places it's shown to, unless rounded otherwise
        final int decimals;

        Display(StatUnit unit, double per, int decimals) {
            this.unit = unit;
            this.per = per;
            this.decimals = decimals;
        }
    }
}
