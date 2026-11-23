package dev.seeruk.bundle.resourcezones;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.addZoneChunk;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

// Zones are shared by the whole test server, so each test uses its own chunk
public class ZoneNoticeGameTest {
    private static final Component NOTICE = Component.literal("In a zone");

    @GameTest
    public void showsTheNoticeInAZone(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(4000, 0)));

        helper.assertValueEqual(ZoneNotice.message(player, NOTICE), Optional.of(NOTICE), "notice");
        helper.succeed();
    }

    @GameTest
    public void showsNothingOutsideZones(GameTestHelper helper) {
        final var player = player(helper);
        addZoneChunk(helper, player, new ChunkPos(4010, 0));
        moveTo(player, new ChunkPos(4011, 0));

        helper.assertValueEqual(ZoneNotice.message(player, NOTICE), Optional.empty(), "notice");
        helper.succeed();
    }
}
