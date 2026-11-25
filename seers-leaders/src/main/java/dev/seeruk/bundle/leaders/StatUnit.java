package dev.seeruk.bundle.leaders;

import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * How a category's total is shown, as a number with thousands separators.
 */
public enum StatUnit implements StringRepresentable {
    /** Shown as it is. */
    COUNT("count"),
    /** Distance stats are in centimetres, shown in blocks. */
    DISTANCE("distance"),
    /** Time stats are in ticks, shown in hours to one decimal place. */
    TIME("time");

    public static final Codec<StatUnit> CODEC = StringRepresentable.fromEnum(StatUnit::values);

    private static final double TICKS_PER_HOUR = SharedConstants.TICKS_PER_SECOND * 60 * 60;

    private final String name;

    StatUnit(String name) {
        this.name = name;
    }

    public String format(long value) {
        return switch (this) {
            case COUNT -> String.format(Locale.ROOT, "%,d", value);
            case DISTANCE -> String.format(Locale.ROOT, "%,d", value / 100);
            case TIME -> String.format(Locale.ROOT, "%,.1f", value / TICKS_PER_HOUR);
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
