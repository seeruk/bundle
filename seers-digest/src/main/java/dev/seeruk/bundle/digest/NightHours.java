package dev.seeruk.bundle.digest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.core.time.TimeCodecs;

import java.time.LocalTime;

/**
 * The hours of the night, which night owls play through, e.g. midnight to 6am. They can go past
 * midnight, e.g. 10pm to 4am.
 *
 * @param from When the night starts.
 * @param to   When it ends, which isn't part of it.
 */
public record NightHours(LocalTime from, LocalTime to) {
    // Hours from and to the same time would never contain anything
    public static final Codec<NightHours> CODEC = RecordCodecBuilder.<NightHours>create(instance -> instance.group(
        TimeCodecs.LOCAL_TIME.fieldOf("from").forGetter(NightHours::from),
        TimeCodecs.LOCAL_TIME.fieldOf("to").forGetter(NightHours::to)
    ).apply(instance, NightHours::new)).validate(hours -> hours.from().equals(hours.to())
        ? DataResult.error(() -> "Night hours can't start and end at the same time: " + hours.from())
        : DataResult.success(hours));

    /**
     * Whether the time is during the night.
     */
    public boolean contains(LocalTime time) {
        // Hours that go past midnight are everything from `from` until midnight, then until `to`
        return from.isAfter(to)
            ? !time.isBefore(from) || time.isBefore(to)
            : !time.isBefore(from) && time.isBefore(to);
    }
}
