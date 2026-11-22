package dev.seeruk.bundle.resourcezones.particles;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ZoneOutlineTest {
    private static final ChunkPos CENTRE = new ChunkPos(0, 0);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Edges {
        @Test
        void outlinesEverySideOfALoneChunk() {
            final var edges = ZoneOutline.edges(Set.of(CENTRE), CENTRE, 2);

            assertEquals(
                Set.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST),
                Set.copyOf(edges.stream().map(ZoneOutline.Edge::side).toList())
            );
        }

        @Test
        void leavesOutSidesBetweenZoneChunks() {
            final var east = new ChunkPos(1, 0);

            final var edges = ZoneOutline.edges(Set.of(CENTRE, east), CENTRE, 2);

            assertEquals(6, edges.size());
            assertFalse(edges.contains(new ZoneOutline.Edge(CENTRE, Direction.EAST)), "Outlined a shared side");
            assertFalse(edges.contains(new ZoneOutline.Edge(east, Direction.WEST)), "Outlined a shared side");
        }

        @Test
        void leavesOutChunksBeyondTheRadius() {
            assertEquals(List.of(), ZoneOutline.edges(Set.of(new ChunkPos(3, 0)), CENTRE, 2));
        }

        @Test
        void putsTheNearestEdgesFirst() {
            final var far = new ChunkPos(2, 2);

            final var edges = ZoneOutline.edges(Set.of(far, CENTRE), CENTRE, 2);

            assertEquals(CENTRE, edges.getFirst().chunk());
            assertEquals(far, edges.getLast().chunk());
        }
    }

    @Nested
    class Points {
        private static final ZoneOutline.Edge NORTH = new ZoneOutline.Edge(new ChunkPos(1, 2), Direction.NORTH);

        // Halfway along the north edge, which runs from x 16 to 32 at z 32
        private static final Vec3 EYES = new Vec3(24, 64, 32);

        @Test
        void spreadsParticlesAlongTheWholeSide() {
            final var points = outline(EYES, 64, ParticleIntensity.MEDIUM, ParticleStyle.BAND);

            // A particle every block from end to end (17), on each of 7 rows from 3 below to 3 above
            assertEquals(17 * 7, points.size());
            assertTrue(points.stream().allMatch(point -> point.z == 32), "Particle off the chunk's north side");
            assertEquals(16, points.stream().mapToDouble(point -> point.x).min().orElseThrow());
            assertEquals(32, points.stream().mapToDouble(point -> point.x).max().orElseThrow());
            assertEquals(61, points.stream().mapToDouble(point -> point.y).min().orElseThrow());
            assertEquals(67, points.stream().mapToDouble(point -> point.y).max().orElseThrow());
        }

        @Test
        void spacesParticlesByIntensity() {
            final var low = outline(EYES, 64, ParticleIntensity.LOW, ParticleStyle.BAND);
            final var high = outline(EYES, 64, ParticleIntensity.HIGH, ParticleStyle.BAND);

            assertEquals(9 * 4, low.size());
            assertEquals(33 * 13, high.size());
        }

        @Test
        void wallsReachFurtherWithRowsFurtherApart() {
            final var points = outline(EYES, 64, ParticleIntensity.MEDIUM, ParticleStyle.WALL);

            assertEquals(17 * 13, points.size());
            assertEquals(40, points.stream().mapToDouble(point -> point.y).min().orElseThrow());
            assertEquals(88, points.stream().mapToDouble(point -> point.y).max().orElseThrow());
        }

        @Test
        void onlyOutlinesColumnsWithinTheRadius() {
            final var points = outline(EYES, 4, ParticleIntensity.MEDIUM, ParticleStyle.BAND);

            // From x 20 to 28, each with 7 rows
            assertEquals(9 * 7, points.size());
            assertEquals(20, points.stream().mapToDouble(point -> point.x).min().orElseThrow());
            assertEquals(28, points.stream().mapToDouble(point -> point.x).max().orElseThrow());
        }

        @Test
        void measuresTheRadiusHorizontally() {
            final var eyes = new Vec3(24, 200, 40);

            final var points = outline(eyes, 8, ParticleIntensity.MEDIUM, ParticleStyle.BAND);

            // Only the column straight across from the player is 8 blocks away, however high up they are
            assertEquals(7, points.size());
            assertTrue(points.stream().allMatch(point -> point.x == 24), "Particle beyond the radius");
        }

        private static List<Vec3> outline(Vec3 eyes, int radius, ParticleIntensity intensity, ParticleStyle style) {
            return ZoneOutline.points(List.of(NORTH), eyes, radius, intensity, style);
        }
    }
}
