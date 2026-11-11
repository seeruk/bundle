package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.itemtext.LoreEditor;
import dev.seeruk.bundle.itemtext.SeersItemText;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;

import java.util.Arrays;
import java.util.List;

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
    public void listsLoreWithoutChangingIt(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing"));

        final var result = execute(helper, player, ItemLoreCommand.NAME);

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void addAppendsLore(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1"));

        // We can also cheekily check that spaces are supported here too...
        final var result = execute(helper, player, ItemLoreCommand.NAME + " add Testing with spaces");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1", "Testing with spaces"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setReplacesGivenLine(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " set 2 Testing 3");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1", "Testing 3"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setWithSameLoreSucceeds(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " set 1 Testing 1");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1", "Testing 2"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void insertInsertsLoreAtGivenLine(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " insert 2 Testing 3");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1", "Testing 3", "Testing 2"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void insertWithoutLoreInsertsBlankLine(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " insert 2");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(
            LoreEditor.lines(player.getMainHandItem()),
            List.of(Component.literal("Testing 1"), Component.empty(), Component.literal("Testing 2")),
            "lore"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeRemovesGivenLine(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " remove 1");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 2"), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeWithoutLineRemovesAllLore(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " remove");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), List.of(), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void clearEmptiesGivenLine(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " clear 1");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(
            LoreEditor.lines(player.getMainHandItem()),
            List.of(Component.empty(), Component.literal("Testing 2")),
            "lore"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void editsMainHandItemOnly(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1"));
        player.setItemInHand(InteractionHand.OFF_HAND, withLore("Testing 1"));

        execute(helper, player, ItemLoreCommand.NAME + " add Testing 2");

        helper.assertValueEqual(LoreEditor.lines(player.getOffhandItem()), literals("Testing 1"), "off hand lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void refusesEmptyHand(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();

        final var result = execute(helper, player, ItemLoreCommand.NAME + " add Testing");

        helper.assertValueEqual(result, 0, "command result");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void failedEditReturnsZeroAndDoesNotModifyLore(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1", "Testing 2"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " set 3 Testing 3");

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1", "Testing 2"), "lore");
        helper.succeed();
    }

    @GameTest
    public void refusesSpectators(GameTestHelper helper) throws CommandSyntaxException {
        final var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, withLore("Testing 1"));

        final var result = execute(helper, player, ItemLoreCommand.NAME + " add Testing 2");

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing 1"), "lore");
        helper.succeed();
    }

    @GameTest
    public void refusesNonPlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(ItemLoreCommand.NAME + " add Testing", source);
            helper.fail("Command didn't fail for a non-player");
        } catch (CommandSyntaxException e) {
            helper.succeed();
        }
    }

    private static int execute(GameTestHelper helper, ServerPlayer player, String command) throws CommandSyntaxException {
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        return dispatcher.execute(command, player.createCommandSourceStack().withSuppressedOutput());
    }

    private static ItemStack withLore(String... lines) {
        final var stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.LORE, new ItemLore(literals(lines)));
        return stack;
    }

    private static List<Component> literals(String... lines) {
        return Arrays.stream(lines).<Component>map(Component::literal).toList();
    }
}
