package dev.seeruk.bundle.resourcezones.edit;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.addZoneChunk;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.respawn;

// Zones are shared by the whole test server, so each test uses its own chunks
public class ZoneEditorGameTest {
    private static final EventLog<ResourceKey<Level>> CHANGES = new EventLog<>();

    static {
        ResourceZoneEvents.CHANGED.register((server, dimension) -> CHANGES.record(dimension));
    }

    @GameTest
    public void marksTheChunkThePlayersIn(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = new ChunkPos(7000, 0);
        moveTo(player, chunk);
        ZoneEditor.start(player, EditMode.ADD);

        ZoneEditor.mark(player);

        helper.assertValueEqual(pending(player), Map.of(chunk, true), "pending changes");
        helper.succeed();
    }

    @GameTest
    public void marksNothingWhilePaused(GameTestHelper helper) {
        // In a zone, and paused after removing chunks, so it'd be removed if pausing didn't stop it
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(7010, 0)));
        ZoneEditor.start(player, EditMode.REMOVE);
        ZoneEditor.pause(player);

        ZoneEditor.mark(player);

        helper.assertValueEqual(pending(player), Map.of(), "pending changes");
        helper.succeed();
    }

    @GameTest
    public void committingSavesTheEditInOneChange(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var removed = addZoneChunk(helper, player, new ChunkPos(7020, 0));
        final var added = new ChunkPos(7021, 0);
        ZoneEditor.start(player, EditMode.REMOVE);
        moveTo(player, removed);
        ZoneEditor.mark(player);
        ZoneEditor.start(player, EditMode.ADD);
        moveTo(player, added);
        ZoneEditor.mark(player);
        final var mark = CHANGES.mark();

        ZoneEditor.commit(server, player);

        final var dimension = player.level().dimension();
        helper.assertFalse(ResourceZones.contains(server, dimension, removed), "Removed chunk was still in a zone");
        helper.assertTrue(ResourceZones.contains(server, dimension, added), "Added chunk wasn't in a zone");
        helper.assertValueEqual(CHANGES.matchingSince(mark, changed -> true), List.of(dimension), "changes");
        helper.assertFalse(ZoneEditor.isEditing(player), "Player was still editing");
        helper.succeed();
    }

    @GameTest
    public void lastChangeWins(GameTestHelper helper) {
        // The chunk's removed and added back by others after the player marked it to be removed
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var chunk = addZoneChunk(helper, player, new ChunkPos(7030, 0));
        moveTo(player, chunk);
        ZoneEditor.start(player, EditMode.REMOVE);
        ZoneEditor.mark(player);
        ResourceZones.remove(server, player.level().dimension(), chunk);
        ResourceZones.add(server, player.level().dimension(), chunk);

        ZoneEditor.commit(server, player);

        helper.assertFalse(ResourceZones.contains(server, player.level().dimension(), chunk), "Chunk was in a zone");
        helper.succeed();
    }

    @GameTest
    public void discardingSavesNothing(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var chunk = new ChunkPos(7040, 0);
        moveTo(player, chunk);
        ZoneEditor.start(player, EditMode.ADD);
        ZoneEditor.mark(player);
        helper.assertValueEqual(pending(player), Map.of(chunk, true), "pending changes");

        ZoneEditor.discard(player);

        helper.assertFalse(ResourceZones.contains(server, player.level().dimension(), chunk), "Chunk was in a zone");
        helper.assertFalse(ZoneEditor.isEditing(player), "Player was still editing");
        helper.succeed();
    }

    @GameTest
    public void editsSurviveRespawning(GameTestHelper helper) {
        final var player = player(helper);
        ZoneEditor.start(player, EditMode.ADD);

        helper.assertTrue(ZoneEditor.isEditing(respawn(helper, player)), "Respawned player wasn't editing");
        helper.succeed();
    }

    @GameTest
    public void editsAreThrownAwayOnLeaving(GameTestHelper helper) {
        final var player = player(helper);
        ZoneEditor.start(player, EditMode.ADD);

        player.connection.disconnect(Component.literal("Left the game"));

        helper.assertFalse(ZoneEditor.isEditing(player), "Player was still editing");
        helper.succeed();
    }

    private static Map<ChunkPos, Boolean> pending(ServerPlayer player) {
        return ZoneEditor.edit(player).orElseThrow().in(player.level().dimension());
    }
}
