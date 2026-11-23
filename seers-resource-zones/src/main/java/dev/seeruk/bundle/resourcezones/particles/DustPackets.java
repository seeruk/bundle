package dev.seeruk.bundle.resourcezones.particles;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Stream;

/**
 * Sends dust particles to only one player, so Vanilla clients see them, and other players don't.
 */
final class DustPackets {
    private DustPackets() {}

    /**
     * One dust particle, shown however far away it is.
     */
    static Packet<? super ClientGamePacketListener> dust(DustParticleOptions particle, Vec3 point) {
        return new ClientboundLevelParticlesPacket(particle, true, false, point.x, point.y, point.z, 0, 0, 0, 0, 0);
    }

    /**
     * Sends the particles to the player in one bundle. If there are more than fit in a bundle, the
     * last are left out, so they should be ordered by importance (e.g. nearest first).
     *
     * @param player    The player to send them to.
     * @param particles The particles' packets.
     * @return How many particles were sent.
     */
    static int send(ServerPlayer player, Stream<Packet<? super ClientGamePacketListener>> particles) {
        final var packets = particles.limit(BundlerInfo.BUNDLE_SIZE_LIMIT).toList();
        if (packets.isEmpty()) {
            return 0;
        }

        player.connection.send(new ClientboundBundlePacket(packets));
        return packets.size();
    }
}
