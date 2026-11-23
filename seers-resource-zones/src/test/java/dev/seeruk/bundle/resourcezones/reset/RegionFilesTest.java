package dev.seeruk.bundle.resourcezones.reset;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegionFilesTest {
    private static final ChunkPos STORED = new ChunkPos(1, 0);
    private static final ChunkPos DELETED = new ChunkPos(2, 0);

    @TempDir
    Path folder;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void readsRegionFilesTheGameWrites() throws IOException {
        // Written by the game itself, so a change to its region files' format fails this
        final var negative = new ChunkPos(-1, -33);
        write(STORED);
        write(negative);

        assertEquals(
            Set.of(STORED, negative),
            RegionFiles.stored(folder, Set.of(STORED, DELETED, negative, new ChunkPos(-2, -33)))
        );
    }

    @Test
    void readsWhichChunksAreStored() throws IOException {
        // Chunks are stored after the header, so the file's longer than it
        final var file = new byte[RegionHeaders.SIZE * 2];
        RegionHeadersTest.header(1).get(0, file, 0, RegionHeaders.SIZE);
        Files.write(folder.resolve("r.0.0.mca"), file);

        assertEquals(Set.of(STORED), RegionFiles.stored(folder, Set.of(STORED, DELETED)));
    }

    @Test
    void storesNothingWithoutARegionFile() {
        assertEquals(Set.of(), RegionFiles.stored(folder, Set.of(STORED)));
    }

    @Test
    void keepsChunksInARegionFileThatCantBeRead() throws IOException {
        // A folder can't be read as a file
        Files.createDirectory(folder.resolve("r.0.0.mca"));

        assertEquals(Set.of(STORED, DELETED), RegionFiles.stored(folder, Set.of(STORED, DELETED)));
    }

    /**
     * Writes a chunk to its region file in the folder, as the game does.
     */
    private void write(ChunkPos chunk) throws IOException {
        final var info = new RegionStorageInfo("test", Level.OVERWORLD, "chunk");
        try (
            final var region = new RegionFile(info, folder.resolve(RegionHeaders.fileName(chunk)), folder, true);
            final var data = region.getChunkDataOutputStream(chunk)
        ) {
            data.writeUTF("chunk");
        }
    }
}
