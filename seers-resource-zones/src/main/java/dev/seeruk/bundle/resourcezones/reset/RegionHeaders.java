package dev.seeruk.bundle.resourcezones.reset;

import net.minecraft.world.level.ChunkPos;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads which chunks a region file stores, from its header. The header starts the file, with an
 * int for each of the region's 32 by 32 chunks, which is 0 if the chunk isn't stored (e.g. because
 * MCA Selector deleted it).
 */
public final class RegionHeaders {
    // One int for each of a region's chunks
    public static final int SIZE = 32 * 32 * Integer.BYTES;

    private RegionHeaders() {}

    /**
     * Which of the chunks the header says are stored. A header that's cut short stores nothing.
     *
     * @param header The start of a region file, from its first byte to its limit.
     * @param chunks Chunks in the header's region.
     */
    public static Set<ChunkPos> stored(ByteBuffer header, Set<ChunkPos> chunks) {
        if (header.limit() < SIZE) {
            return Set.of();
        }

        return chunks.stream()
            .filter(chunk -> header.getInt(index(chunk) * Integer.BYTES) != 0)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The name of the region file the chunk's stored in, e.g. {@code r.0.-1.mca}.
     */
    public static String fileName(ChunkPos chunk) {
        return "r.%d.%d.mca".formatted(chunk.getRegionX(), chunk.getRegionZ());
    }

    private static int index(ChunkPos chunk) {
        return chunk.getRegionLocalX() + chunk.getRegionLocalZ() * 32;
    }
}
