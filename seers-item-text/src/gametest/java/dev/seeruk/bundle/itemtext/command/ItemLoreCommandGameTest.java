package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.itemtext.LoreEditor;
import dev.seeruk.bundle.itemtext.SeersItemText;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;

import java.util.List;

import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.itemtext.LoreFixtures.literals;
import static dev.seeruk.bundle.itemtext.LoreFixtures.withLore;

public class ItemLoreCommandGameTest {
    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(
            root.getChild(ItemLoreCommand.NAME) != null,
            "Command not registered as " + ItemLoreCommand.NAME
        );

        helper.assertTrue(
            root.getChild(SeersItemText.MOD_ID + ":" + ItemLoreCommand.NAME) != null,
            "Command not registered as " + SeersItemText.MOD_ID + ":" + ItemLoreCommand.NAME
        );

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void commandOnItsOwnOpensTheEditor(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing"));

        final var result = execute(helper, player, ItemLoreCommand.NAME);

        helper.assertValueEqual(result, 1, "command result");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void editOpensForTheHeldItem(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " edit");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void editRefusesEmptyHand(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();

        final var result = execute(helper, player, ItemLoreCommand.NAME + " edit");

        helper.assertValueEqual(result, 0, "command result");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeRemovesAllLore(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));
        player.setItemInHand(InteractionHand.OFF_HAND, withLore("Testing 1"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " remove");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), List.of(), "lore");
        helper.assertValueEqual(LoreEditor.lines(player.getOffhandItem()), literals("Testing 1"), "off hand lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void refusesEmptyHand(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();

        final var result = execute(helper, player, ItemLoreCommand.NAME + " remove");

        helper.assertValueEqual(result, 0, "command result");
        helper.succeed();
    }

    @GameTest
    public void refusesSpectators(GameTestHelper helper) throws CommandSyntaxException {
        final var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " remove");

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1"), "lore");
        helper.succeed();
    }

    @GameTest
    public void refusesNonPlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(ItemLoreCommand.NAME + " remove", source);
            helper.fail("Command didn't fail for a non-player");
        } catch (CommandSyntaxException e) {
            helper.succeed();
        }
    }
}
