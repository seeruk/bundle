package dev.seeruk.bundle.resourcezones.particles;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZoneMarkersTest {
    private static final ChunkPos CENTRE = new ChunkPos(0, 0);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Markers {
        @Test
        void marksZonesAndChangesByKind() {
            final var zone = new ChunkPos(1, 0);
            final var added = new ChunkPos(2, 0);
            final var removed = new ChunkPos(0, 1);

            final var markers = ZoneMarkers.markers(
                Set.of(zone, removed),
                Map.of(added, true, removed, false),
                CENTRE,
                2
            );

            assertEquals(
                Set.of(
                    new ZoneMarkers.Marker(zone, ZoneMarkers.Kind.ZONE),
                    new ZoneMarkers.Marker(added, ZoneMarkers.Kind.ADDED),
                    new ZoneMarkers.Marker(removed, ZoneMarkers.Kind.REMOVED)
                ),
                Set.copyOf(markers)
            );
        }

        @Test
        void leavesOutChunksBeyondTheRadius() {
            assertEquals(List.of(), ZoneMarkers.markers(Set.of(new ChunkPos(3, 0)), Map.of(), CENTRE, 2));
        }

        @Test
        void putsTheNearestMarkersFirst() {
            final var far = new ChunkPos(2, 2);

            final var markers = ZoneMarkers.markers(Set.of(far, CENTRE), Map.of(), CENTRE, 2);

            assertEquals(CENTRE, markers.getFirst().chunk());
            assertEquals(far, markers.getLast().chunk());
        }
    }

    @Nested
    class Points {
        private static final Vec3 EYES = new Vec3(100, 64, 100);
        private static final ChunkPos CHUNK = new ChunkPos(1, 2);

        @Test
        void putsAZoneMarkerInTheMiddleOfItsChunkBelowTheEyes() {
            final var points = points(ZoneMarkers.Kind.ZONE);

            // A particle every block, from 4 below the eyes up to them
            assertEquals(5, points.size());
            assertTrue(points.stream().allMatch(point -> point.x == 24 && point.z == 40), "Particle off the middle");
            assertEquals(60, points.stream().mapToDouble(point -> point.y).min().orElseThrow());
            assertEquals(64, points.stream().mapToDouble(point -> point.y).max().orElseThrow());
        }

        @Test
        void topsAnAddedMarkerWithAPlus() {
            final var points = points(ZoneMarkers.Kind.ADDED);

            assertEquals(9, points.size());
            assertTrue(points.contains(new Vec3(25, 64, 40)), "No plus on top");
            assertTrue(points.contains(new Vec3(24, 64, 39)), "No plus on top");
        }

        @Test
        void dashesARemovedMarker() {
            final var points = points(ZoneMarkers.Kind.REMOVED);

            assertEquals(List.of(64.0, 62.0, 60.0), points.stream().map(point -> point.y).toList());
        }

        private static List<Vec3> points(ZoneMarkers.Kind kind) {
            return ZoneMarkers.points(new ZoneMarkers.Marker(CHUNK, kind), EYES);
        }
    }
}
