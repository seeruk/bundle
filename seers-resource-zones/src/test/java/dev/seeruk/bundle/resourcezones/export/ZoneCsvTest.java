package dev.seeruk.bundle.resourcezones.export;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZoneCsvTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Contents {
        static Stream<Arguments> shouldWriteEachChunkWithItsRegion() {
            return Stream.of(
                Arguments.of("Writes MCA Selector's own example", new ChunkPos(3, -36), "0;-2;3;-36\n"),
                Arguments.of("Writes a chunk at the origin", new ChunkPos(0, 0), "0;0;0;0\n"),
                Arguments.of("Writes the last chunk of a region", new ChunkPos(31, 31), "0;0;31;31\n"),
                Arguments.of("Writes the first chunk of the next region", new ChunkPos(32, 32), "1;1;32;32\n"),
                Arguments.of("Writes negative regions", new ChunkPos(-1, -33), "-1;-2;-1;-33\n")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldWriteEachChunkWithItsRegion(
            final String description,
            final ChunkPos chunk,
            final String expected
        ) {
            assertEquals(expected, ZoneCsv.contents(Set.of(chunk)));
        }

        @Test
        void sortsChunksByXThenZ() {
            final var chunks = Set.of(new ChunkPos(1, 0), new ChunkPos(0, 1), new ChunkPos(0, -1));

            assertEquals("0;-1;0;-1\n0;0;0;1\n0;0;1;0\n", ZoneCsv.contents(chunks));
        }

        @Test
        void writesNothingWithoutChunks() {
            assertEquals("", ZoneCsv.contents(Set.of()));
        }
    }

    @Nested
    class Path {
        @Test
        void laysOutFilesLikeTheWorldsDimensionsFolder() {
            assertEquals("resource-zones/minecraft/the_nether.csv", ZoneCsv.path("resource-zones", Level.NETHER));
        }

        @Test
        void keepsModdedDimensionsInTheirOwnFolder() {
            final var dimension = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("example", "mining")
            );

            assertEquals("exports/example/mining.csv", ZoneCsv.path("exports", dimension));
        }
    }
}
