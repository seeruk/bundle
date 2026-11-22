package dev.seeruk.bundle.resourcezones.particles;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Enumerates possible particle intensities, including their spacing, and how often they're
 * refreshed.
 */
public enum ParticleIntensity implements StringRepresentable {
    OFF("off", 0, 0),
    LOW("low", 2, 40),
    MEDIUM("medium", 1, 20),
    HIGH("high", 0.5, 5);

    public static final Codec<ParticleIntensity> CODEC = StringRepresentable.fromEnum(ParticleIntensity::values);

    private final String name;
    private final double spacing;
    private final int intervalTicks;

    /**
     * @param name          The name it's set by, in config and commands.
     * @param spacing       How many blocks apart particles are.
     * @param intervalTicks How often particles are shown again.
     */
    ParticleIntensity(String name, double spacing, int intervalTicks) {
        this.name = name;
        this.spacing = spacing;
        this.intervalTicks = intervalTicks;
    }

    public double spacing() {
        return spacing;
    }

    public int intervalTicks() {
        return intervalTicks;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
