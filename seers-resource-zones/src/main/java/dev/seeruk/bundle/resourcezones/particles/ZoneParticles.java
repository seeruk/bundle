package dev.seeruk.bundle.resourcezones.particles;

import dev.seeruk.bundle.core.storage.PlayerStorage;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.UnaryOperator;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.MOD_ID;

/**
 * Outlines resource zones near each player with particles, as each player has chosen (see
 * {@link ParticleSettings}). Particles are only sent to the player they're for, so Vanilla clients
 * see them, and players don't see each other's.
 */
public final class ZoneParticles {
    private static final PlayerStorage<ParticleSettings> SETTINGS = PlayerStorage.create(
        MOD_ID,
        "particle_settings",
        ParticleSettings.CODEC,
        () -> ParticleSettings.DEFAULTS
    );

    private ZoneParticles() {}

    public static ParticleSettings settings(Player player) {
        return SETTINGS.get(player);
    }

    public static void updateSettings(Player player, UnaryOperator<ParticleSettings> update) {
        SETTINGS.update(player, update);
    }

    public static void showAll(MinecraftServer server, ResourceZonesConfig.Particles config) {
        for (final var player : server.getPlayerList().getPlayers()) {
            show(player, server.getTickCount(), config);
        }
    }

    /**
     * Shows the player the outline of zones near them, if it's time to show it again.
     *
     * @param player The player to show it to.
     * @param tick   The current server tick.
     * @param config The server's particle settings.
     * @return How many particles were shown.
     */
    public static int show(ServerPlayer player, int tick, ResourceZonesConfig.Particles config) {
        final var settings = settings(player);
        final var intensity = settings.intensity();
        final var style = settings.style();
        if (intensity == ParticleIntensity.OFF || tick % intensity.intervalTicks() != 0) {
            return 0;
        }

        final var zone = ResourceZones.chunks(player.level().getServer(), player.level().dimension());
        if (zone.isEmpty()) {
            return 0;
        }

        // Every chunk with a side within the radius, which can be one chunk beyond it
        final var chunkRadius = config.radius() / SectionPos.SECTION_SIZE + 1;
        final var edges = ZoneOutline.edges(zone, player.chunkPosition(), chunkRadius);
        final var points = ZoneOutline.points(edges, player.getEyePosition(), config.radius(), intensity, style);
        if (points.isEmpty()) {
            return 0;
        }

        // Nearest edges come first, so if there are more particles than fit in one bundle, the
        // furthest are left out
        final var particle = new DustParticleOptions(config.colour().getValue(), 1);
        final var packets = points.stream()
            .limit(BundlerInfo.BUNDLE_SIZE_LIMIT)
            .<Packet<? super ClientGamePacketListener>>map(point -> new ClientboundLevelParticlesPacket(
                particle, true, false, point.x, point.y, point.z, 0, 0, 0, 0, 0
            ))
            .toList();

        player.connection.send(new ClientboundBundlePacket(packets));
        return packets.size();
    }
}
