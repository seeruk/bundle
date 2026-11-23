package dev.seeruk.bundle.resourcezones;

import com.mojang.serialization.JavaOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ZoneChunksTest {
    private static final ChunkPos CHUNK = new ChunkPos(1, -2);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Codec {
        @Test
        void savesAndLoadsEveryDimension() {
            final var zones = ZoneChunks.EMPTY
                .with(Level.OVERWORLD, CHUNK)
                .with(Level.OVERWORLD, new ChunkPos(0, 0))
                .with(Level.NETHER, CHUNK);

            final var saved = ZoneChunks.CODEC.encodeStart(JavaOps.INSTANCE, zones).getOrThrow();

            assertEquals(zones, ZoneChunks.CODEC.parse(JavaOps.INSTANCE, saved).getOrThrow());
        }
    }

    @Nested
    class With {
        @Test
        void addsTheChunk() {
            assertTrue(ZoneChunks.EMPTY.with(Level.OVERWORLD, CHUNK).contains(Level.OVERWORLD, CHUNK));
        }

        @Test
        void keepsDimensionsApart() {
            assertFalse(ZoneChunks.EMPTY.with(Level.OVERWORLD, CHUNK).contains(Level.NETHER, CHUNK));
        }

        @Test
        void leavesTheOriginalUnchanged() {
            ZoneChunks.EMPTY.with(Level.OVERWORLD, CHUNK);

            assertEquals(Map.of(), ZoneChunks.EMPTY.chunks());
        }
    }

    @Nested
    class Without {
        @Test
        void removesTheChunk() {
            final var zones = ZoneChunks.EMPTY
                .with(Level.OVERWORLD, CHUNK)
                .with(Level.OVERWORLD, new ChunkPos(0, 0))
                .without(Level.OVERWORLD, CHUNK);

            assertEquals(Map.of(Level.OVERWORLD, Set.of(new ChunkPos(0, 0))), zones.chunks());
        }

        @Test
        void dropsADimensionWithNoChunksLeft() {
            final var zones = ZoneChunks.EMPTY.with(Level.OVERWORLD, CHUNK).without(Level.OVERWORLD, CHUNK);

            assertEquals(ZoneChunks.EMPTY, zones);
        }
    }

    @Nested
    class WithChanges {
        @Test
        void addsAndRemovesTheChunks() {
            final var zones = ZoneChunks.EMPTY
                .with(Level.OVERWORLD, CHUNK)
                .withChanges(Level.OVERWORLD, Set.of(new ChunkPos(0, 0)), Set.of(CHUNK));

            assertEquals(Map.of(Level.OVERWORLD, Set.of(new ChunkPos(0, 0))), zones.chunks());
        }
    }
}
