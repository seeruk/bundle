package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import dev.seeruk.bundle.resourcezones.edit.EditMode;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.*;

// Zones are shared by the whole test server, so each test uses its own chunks
public class ZonePresenceGameTest {
    private static final EventLog<Moved> EVENTS = new EventLog<>();

    private record Moved(UUID player, boolean entered) {}

    static {
        ResourceZoneEvents.PLAYER_ENTERED.register(player -> EVENTS.record(new Moved(player.getUUID(), true)));
        ResourceZoneEvents.PLAYER_LEFT.register(player -> EVENTS.record(new Moved(player.getUUID(), false)));
    }

    @GameTest
    public void joiningInAZoneIsntEntering(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5000, 0)));

        ZonePresence.update(player);

        helper.assertTrue(ZonePresence.isInZone(player), "Player wasn't in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void enteringAZoneFiresAnEvent(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, new ChunkPos(5010, 0));
        ZonePresence.update(player);

        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5011, 0)));
        ZonePresence.update(player);

        helper.assertValueEqual(eventsFor(player), List.of(new Moved(player.getUUID(), true)), "events");
        helper.succeed();
    }

    @GameTest
    public void leavingAZoneFiresAnEvent(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5020, 0)));
        ZonePresence.update(player);

        moveTo(player, new ChunkPos(5021, 0));
        ZonePresence.update(player);

        helper.assertFalse(ZonePresence.isInZone(player), "Player was still in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(new Moved(player.getUUID(), false)), "events");
        helper.succeed();
    }

    @GameTest
    public void movingBetweenZoneChunksIsntEntering(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5030, 0)));
        ZonePresence.update(player);

        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5031, 0)));
        ZonePresence.update(player);

        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void respawningInAZoneTheyWereInIsntEntering(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = addZoneChunk(helper, player, new ChunkPos(5040, 0));
        moveTo(player, chunk);
        ZonePresence.update(player);

        final var respawned = respawn(helper, player);
        moveTo(respawned, chunk);
        ZonePresence.update(respawned);

        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void respawningOutsideTheZoneTheyDiedInIsLeaving(GameTestHelper helper) {
        // The respawned player is a new one, so this only works if they're still known to have been
        // in a zone, rather than seen for the first time
        final var player = player(helper);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5050, 0)));
        ZonePresence.update(player);

        final var respawned = respawn(helper, player);
        moveTo(respawned, new ChunkPos(5051, 0));
        ZonePresence.update(respawned);

        helper.assertValueEqual(eventsFor(player), List.of(new Moved(player.getUUID(), false)), "events");
        helper.succeed();
    }

    @GameTest
    public void aZoneAddedAroundThePlayerIsntEntering(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = new ChunkPos(5060, 0);
        moveTo(player, chunk);
        ZonePresence.update(player);

        addZoneChunk(helper, player, chunk);
        ZonePresence.update(player);

        helper.assertTrue(ZonePresence.isInZone(player), "Player wasn't in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void aZoneRemovedAroundThePlayerIsntLeaving(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = addZoneChunk(helper, player, new ChunkPos(5070, 0));
        moveTo(player, chunk);
        ZonePresence.update(player);

        ResourceZones.remove(helper.getLevel().getServer(), player.level().dimension(), chunk);
        ZonePresence.update(player);

        helper.assertFalse(ZonePresence.isInZone(player), "Player was still in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void spectatorsArentInZones(GameTestHelper helper) {
        final var player = player(helper);
        player.setGameMode(GameType.SPECTATOR);
        moveTo(player, new ChunkPos(5080, 0));
        ZonePresence.update(player);

        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5081, 0)));
        ZonePresence.update(player);

        helper.assertFalse(ZonePresence.isInZone(player), "Spectator was in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void stoppingSpectatingInAZoneIsntEntering(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, new ChunkPos(5090, 0));
        ZonePresence.update(player);
        player.setGameMode(GameType.SPECTATOR);
        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5091, 0)));
        ZonePresence.update(player);

        player.setGameMode(GameType.SURVIVAL);
        ZonePresence.update(player);

        helper.assertTrue(ZonePresence.isInZone(player), "Player wasn't in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void editorsArentInZones(GameTestHelper helper) {
        final var player = player(helper);
        ZoneEditor.start(player, EditMode.ADD);
        moveTo(player, new ChunkPos(5100, 0));
        ZonePresence.update(player);

        moveTo(player, addZoneChunk(helper, player, new ChunkPos(5101, 0)));
        ZonePresence.update(player);

        helper.assertFalse(ZonePresence.isInZone(player), "Editor was in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    @GameTest
    public void finishingAnEditInAZoneIsntEntering(GameTestHelper helper) {
        // They were last seen outside a zone before editing, so this only works if they're seen for
        // the first time after editing
        final var player = player(helper);
        final var zone = addZoneChunk(helper, player, new ChunkPos(5110, 0));
        moveTo(player, new ChunkPos(5111, 0));
        ZonePresence.update(player);
        ZoneEditor.start(player, EditMode.PAUSED);
        moveTo(player, zone);
        ZonePresence.update(player);

        ZoneEditor.discard(player);
        ZonePresence.update(player);

        helper.assertTrue(ZonePresence.isInZone(player), "Player wasn't in a zone");
        helper.assertValueEqual(eventsFor(player), List.of(), "events");
        helper.succeed();
    }

    private static List<Moved> eventsFor(ServerPlayer player) {
        return EVENTS.matching(event -> event.player().equals(player.getUUID()));
    }
}
