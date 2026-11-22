package dev.seeruk.bundle.resourcezones.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.SeersResourceZones;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

import static dev.seeruk.bundle.core.testing.TestCommands.adminSource;
import static dev.seeruk.bundle.core.testing.TestCommands.executeAsAdmin;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

// Zones are shared by the whole test server, so each test uses its own chunk
public class ResourceZonesCommandGameTest {
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
    public void onlyAdminsCanUseIt(GameTestHelper helper) {
        final var player = player(helper);
        final var command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
            .getChild(ResourceZonesCommand.NAME);

        helper.assertFalse(command.canUse(player.createCommandSourceStack()), "Player could use it");
        helper.assertTrue(command.canUse(adminSource(player)), "Admin couldn't use it");
        helper.succeed();
    }

    @GameTest
    public void addsTheCurrentChunk(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        player.teleportTo(2000 * 16 + 8, 100, 8);

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
}
