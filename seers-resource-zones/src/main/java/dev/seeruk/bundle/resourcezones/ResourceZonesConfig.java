package dev.seeruk.bundle.resourcezones;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.TextColor;

/**
 * @param particles How resource zones are outlined with particles.
 */
public record ResourceZonesConfig(Particles particles) {
    public static final Codec<ResourceZonesConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Particles.CODEC.fieldOf("particles").forGetter(ResourceZonesConfig::particles)
    ).apply(instance, ResourceZonesConfig::new));

    /**
     * @param colour The particles' colour.
     * @param radius How many blocks away from a player, horizontally, zones are outlined.
     */
    public record Particles(TextColor colour, int radius) {
        public static final Codec<Particles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextColor.CODEC.fieldOf("colour").forGetter(Particles::colour),
            Codec.intRange(1, 64).fieldOf("radius").forGetter(Particles::radius)
        ).apply(instance, Particles::new));
    }
}
