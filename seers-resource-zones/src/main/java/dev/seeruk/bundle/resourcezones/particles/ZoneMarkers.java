package dev.seeruk.bundle.resourcezones.particles;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out where the markers shown while editing resource zones go. Each chunk that's in a zone,
 * or is being added or removed, gets a column of particles in its middle. Each kind of marker has
 * its own shape, so they can be told apart without their colours.
 */
public final class ZoneMarkers {
    // How many blocks below the player's eyes a marker's column starts
    private static final int HEIGHT = 4;

    // How far the arms of an added chunk's plus reach from the middle of its column
    private static final double ARM = 1;

    private ZoneMarkers() {}

    public enum Kind {
        /**
         * In a zone, and not changed. A plain column.
         */
        ZONE,

        /**
         * Being added to zones. A column with a plus on top.
         */
        ADDED,

        /**
         * Being removed from zones. A dashed column.
         */
        REMOVED
    }

    /**
     * @param chunk The chunk that's marked.
     * @param kind  What kind of marker it is.
     */
    public record Marker(ChunkPos chunk, Kind kind) {}

    /**
     * The markers for chunks near the given chunk, nearest first.
     *
     * @param saved   The chunks in zones now, in one dimension.
     * @param pending The chunks being changed, in the same dimension, each mapped to whether it's
     *                being added (or removed).
     * @param centre  The chunk to look around, e.g. the one a player's in.
     * @param radius  How many chunks away from the centre to look.
     */
    public static List<Marker> markers(
        Set<ChunkPos> saved,
        Map<ChunkPos, Boolean> pending,
        ChunkPos centre,
        int radius
    ) {
        final var markers = new ArrayList<Marker>();
        for (var x = centre.x() - radius; x <= centre.x() + radius; x++) {
            for (var z = centre.z() - radius; z <= centre.z() + radius; z++) {
                final var chunk = new ChunkPos(x, z);
                final var added = pending.get(chunk);
                if (added != null) {
                    markers.add(new Marker(chunk, added ? Kind.ADDED : Kind.REMOVED));
                } else if (saved.contains(chunk)) {
                    markers.add(new Marker(chunk, Kind.ZONE));
                }
            }
        }

        markers.sort(Comparator.comparingInt(marker -> marker.chunk().distanceSquared(centre)));
        return markers;
    }

    /**
     * Where the marker's particles go, in the middle of its chunk, from a few blocks below the
     * player's eyes up to them.
     *
     * @param marker The marker.
     * @param eyes   Where the player's eyes are.
     */
    public static List<Vec3> points(Marker marker, Vec3 eyes) {
        final var top = new Vec3(
            marker.chunk().getMinBlockX() + SectionPos.SECTION_SIZE / 2.0,
            eyes.y,
            marker.chunk().getMinBlockZ() + SectionPos.SECTION_SIZE / 2.0
        );

        final var points = new ArrayList<Vec3>();
        for (var i = 0; i <= HEIGHT; i++) {
            if (marker.kind() != Kind.REMOVED || i % 2 == 0) {
                points.add(top.add(0, -i, 0));
            }
        }

        if (marker.kind() == Kind.ADDED) {
            points.add(top.add(ARM, 0, 0));
            points.add(top.add(-ARM, 0, 0));
            points.add(top.add(0, 0, ARM));
            points.add(top.add(0, 0, -ARM));
        }

        return points;
    }
}
