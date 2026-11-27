package dev.seeruk.bundle.digest;

import com.mojang.serialization.Codec;
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
    public static final Codec<NightHours> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TimeCodecs.LOCAL_TIME.fieldOf("from").forGetter(NightHours::from),
        TimeCodecs.LOCAL_TIME.fieldOf("to").forGetter(NightHours::to)
    ).apply(instance, NightHours::new));

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
