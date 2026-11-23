package dev.seeruk.bundle.resourcezones.reset;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.LOGGER;

/**
 * Checks which chunks are stored in a dimension's region files, by reading their headers (see
 * {@link RegionHeaders}).
 */
public final class RegionFiles {
    private RegionFiles() {}

    /**
     * The folder the dimension's region files are in.
     */
    public static Path folder(MinecraftServer server, ResourceKey<Level> dimension) {
        return DimensionType.getStorageFolder(dimension, server.getWorldPath(LevelResource.ROOT)).resolve("region");
    }

    /**
     * Which of the chunks are stored in the folder's region files. A missing region file stores
     * nothing. If a region file can't be read, its chunks count as stored, so an error is never
     * mistaken for a reset.
     *
     * @param folder The folder the region files are in.
     * @param chunks The chunks to check.
     */
    public static Set<ChunkPos> stored(Path folder, Set<ChunkPos> chunks) {
        final var stored = new HashSet<ChunkPos>();
        final var byRegion = chunks.stream().collect(Collectors.groupingBy(
            RegionHeaders::fileName,
            Collectors.toSet()
        ));
        byRegion.forEach((fileName, inRegion) -> {
            final var path = folder.resolve(fileName);
            try {
                stored.addAll(RegionHeaders.stored(header(path), inRegion));
            } catch (NoSuchFileException e) {
                // Nothing's stored in a region without a file
            } catch (IOException e) {
                LOGGER.warn("Couldn't read the region file {}, so its chunks weren't checked for resets", path, e);
                stored.addAll(inRegion);
            }
        });

        return stored;
    }

    private static ByteBuffer header(Path path) throws IOException {
        try (final var channel = FileChannel.open(path, StandardOpenOption.READ)) {
            final var header = ByteBuffer.allocate(RegionHeaders.SIZE);
            while (header.hasRemaining() && channel.read(header) >= 0) {
                // Keep reading until the header's full, or the file ends
            }

            return header.flip();
        }
    }
}
