package dev.seeruk.bundle.resourcezones.particles;

import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import dev.seeruk.bundle.resourcezones.edit.EditMode;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.ChunkPos;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.addZoneChunk;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

// Zones are shared by the whole test server, so each test uses its own chunk, far from the others
public class EditMarkersGameTest {
    private static final ResourceZonesConfig.Markers CONFIG = new ResourceZonesConfig.Markers(
        8,
        new ResourceZonesConfig.MarkerColours(
            TextColor.fromLegacyFormat(ChatFormatting.BLUE),
            TextColor.fromLegacyFormat(ChatFormatting.GREEN),
            TextColor.fromLegacyFormat(ChatFormatting.RED)
        )
    );

    @GameTest
    public void marksChunksWhileEditing(GameTestHelper helper) {
        // Particles are off for new players, which doesn't stop the markers
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(8000, 0)));
        ZoneEditor.start(player, EditMode.ADD);
        moveTo(player, new ChunkPos(8001, 0));
        ZoneEditor.mark(player);

        // A zone marker (5 particles) and an added marker (9 particles)
        helper.assertValueEqual(EditMarkers.show(player, 0, CONFIG), 5 + 9, "particles");
        helper.succeed();
    }

    @GameTest
    public void marksChunksWhilePaused(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(8100, 0)));
        ZoneEditor.start(player, EditMode.ADD);
        ZoneEditor.pause(player);

        helper.assertValueEqual(EditMarkers.show(player, 0, CONFIG), 5, "particles");
        helper.succeed();
    }

    @GameTest
    public void marksNothingWithoutAnEdit(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(8200, 0)));

        helper.assertValueEqual(EditMarkers.show(player, 0, CONFIG), 0, "particles");
        helper.succeed();
    }

    @GameTest
    public void marksNothingBetweenIntervals(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(8300, 0)));
        ZoneEditor.start(player, EditMode.ADD);

        helper.assertValueEqual(EditMarkers.show(player, 1, CONFIG), 0, "particles");
        helper.succeed();
    }

    @GameTest
    public void marksNoMoreOftenWithHighParticles(GameTestHelper helper) {
        // High particles are refreshed every 5 ticks
        final var player = player(helper);
        ZoneParticles.updateSettings(player, settings -> settings.withIntensity(ParticleIntensity.HIGH));
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(8400, 0)));
        ZoneEditor.start(player, EditMode.ADD);

        helper.assertValueEqual(EditMarkers.show(player, 5, CONFIG), 0, "particles");
        helper.succeed();
    }
}
