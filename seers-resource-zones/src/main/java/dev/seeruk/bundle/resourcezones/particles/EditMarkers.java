package dev.seeruk.bundle.resourcezones.particles;

import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;

/**
 * Marks chunks near players who are editing resource zones (see {@link ZoneMarkers}), so they can
 * see their changes from much further away than zones' outlines.
 */
public final class EditMarkers {
    // Dust is too small to see from far away at its usual size
    private static final float SCALE = 2;

    // Markers don't move, so they're refreshed no faster than this, however intense players' own
    // particles are. Dust lives longer the larger it is, so more often would pile up particles.
    private static final int INTERVAL_TICKS = 20;

    private EditMarkers() {}

    public static void showAll(MinecraftServer server, ResourceZonesConfig.Markers config) {
        for (final var player : server.getPlayerList().getPlayers()) {
            show(player, server.getTickCount(), config);
        }
    }

    /**
     * Shows the player markers on the chunks near them, if they're editing (or have paused editing),
     * and it's time to show them again. They're shown whatever the player's own particle settings.
     *
     * @param player The player to show them to.
     * @param tick   The current server tick.
     * @param config The server's marker settings.
     * @return How many particles were shown.
     */
    public static int show(ServerPlayer player, int tick, ResourceZonesConfig.Markers config) {
        final var edit = ZoneEditor.edit(player);
        if (edit.isEmpty() || tick % INTERVAL_TICKS != 0) {
            return 0;
        }

        final var level = player.level();
        final var markers = ZoneMarkers.markers(
            ResourceZones.chunks(level.getServer(), level.dimension()),
            edit.get().in(level.dimension()),
            player.chunkPosition(),
            config.radiusChunks()
        );

        final var particles = new EnumMap<ZoneMarkers.Kind, DustParticleOptions>(ZoneMarkers.Kind.class);
        for (final var kind : ZoneMarkers.Kind.values()) {
            particles.put(kind, new DustParticleOptions(colour(config.colours(), kind).getValue(), SCALE));
        }

        // Nearest markers come first, so if there are too many particles, the furthest are left out
        final var eyes = player.getEyePosition();
        return DustPackets.send(
            player, markers.stream().flatMap(marker -> ZoneMarkers.points(marker, eyes).stream()
                .map(point -> DustPackets.dust(particles.get(marker.kind()), point)))
        );
    }

    /**
     * The configured colour for the kind of marker.
     */
    private static TextColor colour(ResourceZonesConfig.MarkerColours colours, ZoneMarkers.Kind kind) {
        return switch (kind) {
            case ZONE -> colours.zone();
            case ADDED -> colours.added();
            case REMOVED -> colours.removed();
        };
    }
}
