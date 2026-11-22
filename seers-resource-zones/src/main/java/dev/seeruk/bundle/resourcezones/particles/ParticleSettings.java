package dev.seeruk.bundle.resourcezones.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's own particle settings.
 *
 * @param intensity The intensity of the particles.
 * @param style     How the particles are distributed to mark zones.
 */
public record ParticleSettings(ParticleIntensity intensity, ParticleStyle style) {
    // By default, particles are off until a player turns them on.
    public static final ParticleSettings DEFAULTS = new ParticleSettings(ParticleIntensity.OFF, ParticleStyle.BAND);

    public static final Codec<ParticleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ParticleIntensity.CODEC.optionalFieldOf("intensity", DEFAULTS.intensity())
            .forGetter(ParticleSettings::intensity),
        ParticleStyle.CODEC.optionalFieldOf("style", DEFAULTS.style())
            .forGetter(ParticleSettings::style)
    ).apply(instance, ParticleSettings::new));

    public ParticleSettings withIntensity(ParticleIntensity intensity) {
        return new ParticleSettings(intensity, style);
    }

    public ParticleSettings withStyle(ParticleStyle style) {
        return new ParticleSettings(intensity, style);
    }
}
