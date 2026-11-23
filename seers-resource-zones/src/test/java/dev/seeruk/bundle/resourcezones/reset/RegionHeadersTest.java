package dev.seeruk.bundle.resourcezones.reset;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegionHeadersTest {
    @Nested
    class Stored {
        @Test
        void findsChunksWithAnEntry() {
            final var stored = new ChunkPos(1, 0);
            final var header = header(1);

            assertEquals(Set.of(stored), RegionHeaders.stored(header, Set.of(stored, new ChunkPos(2, 0))));
        }

        @Test
        void findsNegativeChunksByTheirPlaceInTheRegion() {
            // The last chunk of region -1, -1
            final var chunk = new ChunkPos(-1, -1);

            assertEquals(Set.of(chunk), RegionHeaders.stored(header(1023), Set.of(chunk)));
        }

        @Test
        void storesNothingInAShortHeader() {
            final var header = header(1).limit(100);

            assertEquals(Set.of(), RegionHeaders.stored(header, Set.of(new ChunkPos(1, 0))));
        }
    }

    @Test
    void namesTheRegionFile() {
        assertEquals("r.-1.1.mca", RegionHeaders.fileName(new ChunkPos(-1, 32)));
    }

    /**
     * A header with only the given entries set.
     */
    static ByteBuffer header(int... entries) {
        final var header = ByteBuffer.allocate(RegionHeaders.SIZE);
        for (final var entry : entries) {
            header.putInt(entry * Integer.BYTES, 0x201);
        }

        return header;
    }
}
