package dev.seeruk.bundle.resourcezones.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.SeersResourceZones;
import dev.seeruk.bundle.resourcezones.edit.EditMode;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import dev.seeruk.bundle.resourcezones.particles.ParticleIntensity;
import dev.seeruk.bundle.resourcezones.particles.ParticleStyle;
import dev.seeruk.bundle.resourcezones.particles.ZoneParticles;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;

import static dev.seeruk.bundle.core.testing.TestCommands.adminSource;
import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestCommands.executeAsAdmin;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

// Zones are shared by the whole test server, so each test uses its own chunk
public class ResourceZonesCommandGameTest {
    private static final String PARTICLES = ResourceZonesCommand.NAME + " particles";
    private static final String EDIT = ResourceZonesCommand.NAME + " edit";

    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(
            root.getChild(ResourceZonesCommand.NAME) != null,
            "Command not registered as " + ResourceZonesCommand.NAME
        );
        helper.assertTrue(
            root.getChild(SeersResourceZones.MOD_ID + ":" + ResourceZonesCommand.NAME) != null,
            "Command not registered as " + SeersResourceZones.MOD_ID + ":" + ResourceZonesCommand.NAME
        );
        helper.succeed();
    }

    @GameTest
    public void onlyAdminsCanChangeZones(GameTestHelper helper) {
        final var player = player(helper);
        final var command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
            .getChild(ResourceZonesCommand.NAME);

        for (final var subcommand : List.of("add", "remove", "edit")) {
            final var node = command.getChild(subcommand);
            helper.assertFalse(node.canUse(player.createCommandSourceStack()), "Player could use " + subcommand);
            helper.assertTrue(node.canUse(adminSource(player)), "Admin couldn't use " + subcommand);
        }
        helper.succeed();
    }

    @GameTest
    public void addsTheCurrentChunk(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        moveTo(player, new ChunkPos(2000, 0));

        final var result = executeAsAdmin(helper, player, ResourceZonesCommand.NAME + " add");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertTrue(
            ResourceZones.contains(helper.getLevel().getServer(), player.level().dimension(), new ChunkPos(2000, 0)),
            "Chunk wasn't in a zone"
        );
        helper.succeed();
    }

    @GameTest
    public void addsTheChunkWithTheGivenBlock(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        final var result = executeAsAdmin(helper, player, ResourceZonesCommand.NAME + " add 32017 -15");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertTrue(
            ResourceZones.contains(helper.getLevel().getServer(), player.level().dimension(), new ChunkPos(2001, -1)),
            "Chunk wasn't in a zone"
        );
        helper.succeed();
    }

    @GameTest
    public void addingAChunkAlreadyInAZoneFails(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        ResourceZones.add(helper.getLevel().getServer(), player.level().dimension(), new ChunkPos(2002, 0));

        final var result = executeAsAdmin(helper, player, ResourceZonesCommand.NAME + " add 32032 0");

        helper.assertValueEqual(result, 0, "command result");
        helper.succeed();
    }

    @GameTest
    public void removesTheChunk(GameTestHelper helper) throws CommandSyntaxException {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        ResourceZones.add(server, player.level().dimension(), new ChunkPos(2003, 0));

        final var result = executeAsAdmin(helper, player, ResourceZonesCommand.NAME + " remove 32048 0");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertFalse(
            ResourceZones.contains(server, player.level().dimension(), new ChunkPos(2003, 0)),
            "Chunk was still in a zone"
        );
        helper.succeed();
    }

    @GameTest
    public void removingAChunkNotInAZoneFails(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        final var result = executeAsAdmin(helper, player, ResourceZonesCommand.NAME + " remove 32064 0");

        helper.assertValueEqual(result, 0, "command result");
        helper.succeed();
    }

    @GameTest
    public void actingOnTheCurrentChunkNeedsAnEntity(GameTestHelper helper) {
        // The console runs commands from the world spawn, which it shouldn't add to a zone
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        for (final var subcommand : List.of("add", "remove")) {
            try {
                server.getCommands().getDispatcher().execute(ResourceZonesCommand.NAME + " " + subcommand, source);
                helper.fail("The console could " + subcommand + " the current chunk");
            } catch (CommandSyntaxException e) {
                // Expected
            }
        }

        helper.assertFalse(
            ResourceZones.contains(server, Level.OVERWORLD, ChunkPos.containing(server.getRespawnData().pos())),
            "Spawn chunk was in a zone"
        );
        helper.succeed();
    }

    @GameTest
    public void startsAndPausesEditing(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        helper.assertValueEqual(executeAsAdmin(helper, player, EDIT + " remove"), 1, "remove result");
        helper.assertValueEqual(mode(player), EditMode.REMOVE, "mode");
        helper.assertValueEqual(executeAsAdmin(helper, player, EDIT + " pause"), 1, "pause result");
        helper.assertValueEqual(mode(player), EditMode.PAUSED, "mode");
        helper.assertValueEqual(executeAsAdmin(helper, player, EDIT + " add"), 1, "add result");
        helper.assertValueEqual(mode(player), EditMode.ADD, "mode");
        helper.assertValueEqual(executeAsAdmin(helper, player, EDIT), 1, "describe result");
        helper.succeed();
    }

    @GameTest
    public void commitsTheEdit(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var chunk = new ChunkPos(2010, 0);
        moveTo(player, chunk);
        executeAsAdmin(helper, player, EDIT + " add");
        ZoneEditor.mark(player);

        final var result = executeAsAdmin(helper, player, EDIT + " commit");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertTrue(
            ResourceZones.contains(helper.getLevel().getServer(), player.level().dimension(), chunk),
            "Chunk wasn't in a zone"
        );
        helper.assertFalse(ZoneEditor.isEditing(player), "Player was still editing");
        helper.succeed();
    }

    @GameTest
    public void discardsTheEdit(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var chunk = new ChunkPos(2020, 0);
        moveTo(player, chunk);
        executeAsAdmin(helper, player, EDIT + " add");
        ZoneEditor.mark(player);

        final var result = executeAsAdmin(helper, player, EDIT + " discard");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertFalse(
            ResourceZones.contains(helper.getLevel().getServer(), player.level().dimension(), chunk),
            "Chunk was in a zone"
        );
        helper.assertFalse(ZoneEditor.isEditing(player), "Player was still editing");
        helper.succeed();
    }

    @GameTest
    public void editingCommandsFailWithoutAnEdit(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        for (final var subcommand : List.of("", " pause", " commit", " discard")) {
            helper.assertValueEqual(
                executeAsAdmin(helper, player, EDIT + subcommand),
                0,
                "edit" + subcommand + " result"
            );
        }
        helper.succeed();
    }

    @GameTest
    public void showsParticleSettings(GameTestHelper helper) throws CommandSyntaxException {
        helper.assertValueEqual(execute(helper, player(helper), PARTICLES), 1, "command result");
        helper.succeed();
    }

    @GameTest
    public void setsParticleIntensity(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        final var result = execute(helper, player, PARTICLES + " low");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(ZoneParticles.settings(player).intensity(), ParticleIntensity.LOW, "intensity");
        helper.succeed();
    }

    @GameTest
    public void turnsParticlesOff(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        execute(helper, player, PARTICLES + " low");

        execute(helper, player, PARTICLES + " off");

        helper.assertValueEqual(ZoneParticles.settings(player).intensity(), ParticleIntensity.OFF, "intensity");
        helper.succeed();
    }

    @GameTest
    public void setsParticleStyle(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        final var result = execute(helper, player, PARTICLES + " style wall");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(ZoneParticles.settings(player).style(), ParticleStyle.WALL, "style");
        helper.succeed();
    }

    private static EditMode mode(ServerPlayer player) {
        return ZoneEditor.edit(player).orElseThrow().mode();
    }
}
