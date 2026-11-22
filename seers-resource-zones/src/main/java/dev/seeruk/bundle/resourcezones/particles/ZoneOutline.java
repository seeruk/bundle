package dev.seeruk.bundle.resourcezones.particles;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Works out where the particles outlining resource zones go. Only the outside of a zone is
 * outlined, i.e. the sides of zone chunks that meet chunks that aren't in a zone.
 */
public final class ZoneOutline {
    private static final List<Direction> SIDES = Direction.Plane.HORIZONTAL.stream().toList();

    private ZoneOutline() {}

    /**
     * One side of a zone chunk that's on the outside of a zone.
     *
     * @param chunk The zone chunk.
     * @param side  Which of its sides it is.
     */
    public record Edge(ChunkPos chunk, Direction side) {}

    /**
     * The edges of zone chunks near the given chunk, nearest first.
     *
     * @param zone   The chunks in zones, in one dimension.
     * @param centre The chunk to look around, e.g. the one a player's in.
     * @param radius How many chunks away from the centre to look.
     */
    public static List<Edge> edges(Set<ChunkPos> zone, ChunkPos centre, int radius) {
        final var edges = new ArrayList<Edge>();
        for (var x = centre.x() - radius; x <= centre.x() + radius; x++) {
            for (var z = centre.z() - radius; z <= centre.z() + radius; z++) {
                final var chunk = new ChunkPos(x, z);
                if (!zone.contains(chunk)) {
                    continue;
                }

                for (final var side : SIDES) {
                    if (!zone.contains(new ChunkPos(x + side.getStepX(), z + side.getStepZ()))) {
                        edges.add(new Edge(chunk, side));
                    }
                }
            }
        }

        edges.sort(Comparator.comparingInt(edge -> edge.chunk().distanceSquared(centre)));
        return edges;
    }

    /**
     * Where particles go along each edge, as a grid on the chunk's side, from the bottom to the top
     * of the style's reach around the height of the player's eyes. Only columns within the radius
     * of the player (horizontally) get particles, so the outline follows them around.
     *
     * @param edges     The edges to outline.
     * @param eyes      Where the player's eyes are.
     * @param radius    How many blocks away from the player, horizontally, particles go.
     * @param intensity How far apart particles are.
     * @param style     How far up and down particles go.
     */
    public static List<Vec3> points(
        List<Edge> edges,
        Vec3 eyes,
        int radius,
        ParticleIntensity intensity,
        ParticleStyle style
    ) {
        final var across = steps(SectionPos.SECTION_SIZE, intensity.spacing());
        final var up = steps(style.reach() * 2, intensity.spacing() * style.verticalSpread());
        final var bottom = eyes.y - style.reach();

        final var points = new ArrayList<Vec3>();
        for (final var edge : edges) {
            for (var i = 0; i <= across; i++) {
                final var column = point(edge, i * (double) SectionPos.SECTION_SIZE / across, bottom);
                final var x = column.x - eyes.x;
                final var z = column.z - eyes.z;
                if (x * x + z * z > radius * radius) {
                    continue;
                }

                for (var j = 0; j <= up; j++) {
                    points.add(column.add(0, j * (style.reach() * 2.0) / up, 0));
                }
            }
        }

        return points;
    }

    /**
     * How many gaps there are between particles over the length, as near to the spacing as they
     * can be while spreading evenly from one end to the other.
     */
    private static int steps(double length, double spacing) {
        return Math.max(1, (int) Math.round(length / spacing));
    }

    /**
     * The point on the edge's side of the chunk, the given distance along it from its west or north
     * end.
     */
    private static Vec3 point(Edge edge, double along, double y) {
        final var minX = edge.chunk().getMinBlockX();
        final var minZ = edge.chunk().getMinBlockZ();

        return switch (edge.side()) {
            case NORTH -> new Vec3(minX + along, y, minZ);
            case SOUTH -> new Vec3(minX + along, y, minZ + SectionPos.SECTION_SIZE);
            case WEST -> new Vec3(minX, y, minZ + along);
            case EAST -> new Vec3(minX + SectionPos.SECTION_SIZE, y, minZ + along);
            default -> throw new IllegalArgumentException("Not a horizontal side: " + edge.side());
        };
    }
}
