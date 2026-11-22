package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;

// Zones are shared by the whole test server, so each test uses its own chunk
public class ResourceZonesGameTest {
    private static final EventLog<ResourceKey<Level>> EVENTS = new EventLog<>();

    static {
        ResourceZoneEvents.CHANGED.register((server, dimension) -> EVENTS.record(dimension));
    }

    @GameTest
    public void addPutsTheChunkInAZone(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var chunk = new ChunkPos(1000, 0);

        helper.assertTrue(ResourceZones.add(server, Level.OVERWORLD, chunk), "Add didn't change anything");
        helper.assertTrue(ResourceZones.contains(server, Level.OVERWORLD, chunk), "Chunk wasn't in a zone");
        helper.succeed();
    }

    @GameTest
    public void addingAChunkAlreadyInAZoneChangesNothing(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var chunk = new ChunkPos(1001, 0);
        ResourceZones.add(server, Level.OVERWORLD, chunk);

        helper.assertFalse(ResourceZones.add(server, Level.OVERWORLD, chunk), "Adding again changed something");
        helper.succeed();
    }

    @GameTest
    public void removeTakesTheChunkOutOfZones(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var chunk = new ChunkPos(1002, 0);
        ResourceZones.add(server, Level.OVERWORLD, chunk);

        helper.assertTrue(ResourceZones.remove(server, Level.OVERWORLD, chunk), "Remove didn't change anything");
        helper.assertFalse(ResourceZones.contains(server, Level.OVERWORLD, chunk), "Chunk was still in a zone");
        helper.succeed();
    }

    @GameTest
    public void removingAChunkNotInAZoneChangesNothing(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();

        helper.assertFalse(
            ResourceZones.remove(server, Level.OVERWORLD, new ChunkPos(1003, 0)),
            "Removing changed something"
        );
        helper.succeed();
    }

    @GameTest
    public void changesFireAnEventForTheirDimension(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var chunk = new ChunkPos(1004, 0);
        final var mark = EVENTS.mark();

        ResourceZones.add(server, Level.NETHER, chunk);
        ResourceZones.remove(server, Level.NETHER, chunk);

        helper.assertValueEqual(
            EVENTS.matchingSince(mark, dimension -> true),
            List.of(Level.NETHER, Level.NETHER),
            "events"
        );
        helper.succeed();
    }

    @GameTest
    public void unchangedZonesFireNoEvent(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var chunk = new ChunkPos(1005, 0);
        ResourceZones.add(server, Level.OVERWORLD, chunk);
        final var mark = EVENTS.mark();

        ResourceZones.add(server, Level.OVERWORLD, chunk);
        ResourceZones.remove(server, Level.OVERWORLD, new ChunkPos(1006, 0));

        helper.assertValueEqual(EVENTS.matchingSince(mark, dimension -> true), List.of(), "events");
        helper.succeed();
    }
}
