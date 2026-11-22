package dev.seeruk.bundle.resourcezones.export;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zone chunks as an MCA Selector selection CSV, which MCA Selector can delete the chunks with.
 * Each chunk is a line of its region's coordinates, then its own (absolute) coordinates, e.g.
 * {@code 0;-2;3;-36}.
 */
public final class ZoneCsv {
    private ZoneCsv() {}

    /**
     * A sorted CSV (well, semicolon-separated) list of region coordinates and chunk coordinates.
     * Sorting means the file only changes when the chunks do.
     */
    public static String contents(Set<ChunkPos> chunks) {
        return chunks.stream()
            .sorted(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z))
            .map(chunk -> "%d;%d;%d;%d\n".formatted(chunk.getRegionX(), chunk.getRegionZ(), chunk.x(), chunk.z()))
            .collect(Collectors.joining());
    }

    /**
     * For the given folder and dimension, returns the path to the zone file.
     */
    public static String path(String folder, ResourceKey<Level> dimension) {
        final var id = dimension.identifier();
        return "%s/%s/%s.csv".formatted(folder, id.getNamespace(), id.getPath());
    }
}
