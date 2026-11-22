package dev.seeruk.bundle.resourcezones;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.TextColor;

/**
 * @param particles How resource zones are outlined with particles.
 * @param export    Where resource zones are exported for MCA Selector.
 */
public record ResourceZonesConfig(Particles particles, Export export) {
    public static final Codec<ResourceZonesConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Particles.CODEC.fieldOf("particles").forGetter(ResourceZonesConfig::particles),
        Export.CODEC.fieldOf("export").forGetter(ResourceZonesConfig::export)
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

    /**
     * @param folder The folder each dimension's file is written to, relative to the server's folder.
     */
    public record Export(String folder) {
        public static final Codec<Export> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("folder").forGetter(Export::folder)
        ).apply(instance, Export::new));
    }
}
