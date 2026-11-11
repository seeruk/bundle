package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.itemtext.NameEditor;
import dev.seeruk.bundle.itemtext.SeersItemText;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Optional;

public class ItemNameCommandGameTest {
    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(
            root.getChild(ItemNameCommand.NAME) != null,
            "Command not registered as " + ItemNameCommand.NAME
        );

        helper.assertTrue(
            root.getChild(SeersItemText.MOD_ID + ":" + ItemNameCommand.NAME) != null,
            "Command not registered as " + SeersItemText.MOD_ID + ":" + ItemNameCommand.NAME
        );

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setSetsName(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withName("Testing"));

        // We can also cheekily check that spaces are supported here too...
        final var result = execute(helper, player, ItemNameCommand.NAME + " set Testing with spaces");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(NameEditor.customName(player.getMainHandItem()), literal("Testing with spaces"), "name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setWithSameNameSucceeds(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withName("Testing"));

        final var result = execute(helper, player, ItemNameCommand.NAME + " set Testing");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(NameEditor.customName(player.getMainHandItem()), literal("Testing"), "name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void resetRemovesName(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withName("Testing"));

        final var result = execute(helper, player, ItemNameCommand.NAME + " reset");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(NameEditor.customName(player.getMainHandItem()), Optional.empty(), "name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void editsMainHandItemOnly(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withName("Testing 1"));
        player.setItemInHand(InteractionHand.OFF_HAND, withName("Testing 1"));

        execute(helper, player, ItemNameCommand.NAME + " set Testing 2");

        helper.assertValueEqual(NameEditor.customName(player.getOffhandItem()), literal("Testing 1"), "off hand name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void refusesEmptyHand(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();

        final var result = execute(helper, player, ItemNameCommand.NAME + " set Testing");

        helper.assertValueEqual(result, 0, "command result");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void failedEditReturnsZeroAndDoesNotModifyName(GameTestHelper helper) throws CommandSyntaxException {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, withName("Testing"));

        final var result = execute(helper, player, ItemNameCommand.NAME + " set " + "a".repeat(AnvilMenu.MAX_NAME_LENGTH + 1));

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(NameEditor.customName(player.getMainHandItem()), literal("Testing"), "name");
        helper.succeed();
    }

    @GameTest
    public void refusesSpectators(GameTestHelper helper) throws CommandSyntaxException {
        final var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, withName("Testing 1"));

        final var result = execute(helper, player, ItemNameCommand.NAME + " set Testing 2");

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(NameEditor.customName(player.getMainHandItem()), literal("Testing 1"), "name");
        helper.succeed();
    }

    @GameTest
    public void refusesNonPlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(ItemNameCommand.NAME + " set Testing", source);
            helper.fail("Command didn't fail for a non-player");
        } catch (CommandSyntaxException e) {
            helper.succeed();
        }
    }

    private static int execute(GameTestHelper helper, ServerPlayer player, String command) throws CommandSyntaxException {
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        return dispatcher.execute(command, player.createCommandSourceStack().withSuppressedOutput());
    }

    private static ItemStack withName(String name) {
        final var stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static Optional<Component> literal(String name) {
        return Optional.of(Component.literal(name));
    }
}
