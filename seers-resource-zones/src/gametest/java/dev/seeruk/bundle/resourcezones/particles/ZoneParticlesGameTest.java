package dev.seeruk.bundle.resourcezones.particles;

import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.addZoneChunk;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

// Zones are shared by the whole test server, so each test uses its own chunk, far from the others
public class ZoneParticlesGameTest {
    private static final ResourceZonesConfig.Particles CONFIG = new ResourceZonesConfig.Particles(
        TextColor.fromLegacyFormat(ChatFormatting.GOLD),
        16
    );

    @GameTest
    public void showsZonesNearThePlayer(GameTestHelper helper) {
        final var player = playerInZone(helper, new ChunkPos(3000, 0));

        // All 4 sides of a chunk, with 17 particles along each size (at each chunk edge), along
        // 7 rows. Everything around the chunk should be included.
        helper.assertValueEqual(ZoneParticles.show(player, 0, CONFIG), 4 * 17 * 7, "particles");
        helper.succeed();
    }

    @GameTest
    public void showsNothingBetweenIntervals(GameTestHelper helper) {
        final var player = playerInZone(helper, new ChunkPos(3010, 0));

        helper.assertValueEqual(ZoneParticles.show(player, 1, CONFIG), 0, "particles");
        helper.succeed();
    }

    @GameTest
    public void showsNothingWithoutZonesNearby(GameTestHelper helper) {
        final var player = player(helper);
        ZoneParticles.updateSettings(player, settings -> settings.withIntensity(ParticleIntensity.MEDIUM));
        moveTo(player, new ChunkPos(3020, 0));

        helper.assertValueEqual(ZoneParticles.show(player, 0, CONFIG), 0, "particles");
        helper.succeed();
    }

    @GameTest
    public void showsNothingUntilThePlayerTurnsThemOn(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(3030, 0)));

        helper.assertValueEqual(ZoneParticles.show(player, 0, CONFIG), 0, "particles");
        helper.succeed();
    }

    @GameTest
    public void usesThePlayersOwnSettings(GameTestHelper helper) {
        final var player = playerInZone(helper, new ChunkPos(3040, 0));
        ZoneParticles.updateSettings(player, settings -> settings.withStyle(ParticleStyle.WALL));

        // Walls have 13 rows instead of 7
        helper.assertValueEqual(ZoneParticles.show(player, 0, CONFIG), 4 * 17 * 13, "particles");
        helper.succeed();
    }

    /**
     * A player with particles on, standing in the given chunk, which is the only zone chunk near
     * them.
     */
    private static ServerPlayer playerInZone(GameTestHelper helper, ChunkPos chunk) {
        final var player = player(helper);
        ZoneParticles.updateSettings(player, settings -> settings.withIntensity(ParticleIntensity.MEDIUM));
        moveTo(player, addZoneChunk(helper, player, chunk));
        return player;
    }
}
