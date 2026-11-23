package dev.seeruk.bundle.resourcezones;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.resourcezones.particles.ZoneMarkers;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.network.chat.TextColor;

/**
 * @param notice    Shown in the action bar while a player is in a resource zone.
 * @param particles How resource zones are outlined with particles.
 * @param edit      How editing resource zones looks.
 * @param export    Where resource zones are exported for MCA Selector.
 */
public record ResourceZonesConfig(TextTemplate notice, Particles particles, Edit edit, Export export) {
    public static final Codec<ResourceZonesConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TextTemplate.CODEC.fieldOf("notice").forGetter(ResourceZonesConfig::notice),
        Particles.CODEC.fieldOf("particles").forGetter(ResourceZonesConfig::particles),
        Edit.CODEC.fieldOf("edit").forGetter(ResourceZonesConfig::edit),
        Export.CODEC.fieldOf("export").forGetter(ResourceZonesConfig::export)
    ).apply(instance, ResourceZonesConfig::new));

    /**
     * @param colour       The particles' colour.
     * @param radiusBlocks How many blocks away from a player, horizontally, zones are outlined.
     */
    public record Particles(TextColor colour, int radiusBlocks) {
        public static final Codec<Particles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextColor.CODEC.fieldOf("colour").forGetter(Particles::colour),
            Codec.intRange(1, 64).fieldOf("radius_blocks").forGetter(Particles::radiusBlocks)
        ).apply(instance, Particles::new));
    }

    /**
     * @param status  Shown in the action bar while a player is editing resource zones.
     * @param markers The markers on chunks while a player is editing resource zones.
     */
    public record Edit(TextTemplate status, Markers markers) {
        public static final Codec<Edit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextTemplate.CODEC.fieldOf("status").forGetter(Edit::status),
            Markers.CODEC.fieldOf("markers").forGetter(Edit::markers)
        ).apply(instance, Edit::new));
    }

    /**
     * @param radiusChunks How many chunks away from a player chunks are marked.
     * @param colours      Each kind of marker's colour.
     */
    public record Markers(int radiusChunks, MarkerColours colours) {
        public static final Codec<Markers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("radius_chunks").forGetter(Markers::radiusChunks),
            MarkerColours.CODEC.fieldOf("colours").forGetter(Markers::colours)
        ).apply(instance, Markers::new));
    }

    /**
     * @param zone    Chunks in a zone, that aren't changed.
     * @param added   Chunks to be added to zones.
     * @param removed Chunks to be removed from zones.
     */
    public record MarkerColours(TextColor zone, TextColor added, TextColor removed) {
        public static final Codec<MarkerColours> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextColor.CODEC.fieldOf("zone").forGetter(MarkerColours::zone),
            TextColor.CODEC.fieldOf("added").forGetter(MarkerColours::added),
            TextColor.CODEC.fieldOf("removed").forGetter(MarkerColours::removed)
        ).apply(instance, MarkerColours::new));

        public TextColor of(ZoneMarkers.Kind kind) {
            return switch (kind) {
                case ZONE -> zone;
                case ADDED -> added;
                case REMOVED -> removed;
            };
        }
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
