package dev.seeruk.bundle.itemtext.dialog;

import dev.seeruk.bundle.itemtext.LoreEditor;
import dev.seeruk.bundle.itemtext.LoreInput;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.itemtext.LoreFixtures.literals;
import static dev.seeruk.bundle.itemtext.LoreFixtures.withLore;

public class LoreDialogGameTest {
    @GameTest
    public void savingSetsTheHeldItemsLore(GameTestHelper helper) {
        final var player = holding(player(helper), new ItemStack(Items.NETHERITE_SWORD));
        final var input = "<red>Testing 1\nTesting 2</red>";

        save(player, input);

        helper.assertValueEqual(
            plain(LoreEditor.lines(player.getMainHandItem())),
            List.of("Testing 1", "Testing 2"),
            "lore"
        );
        helper.assertValueEqual(LoreInput.get(player.getMainHandItem()), Optional.of(input), "remembered input");
        helper.succeed();
    }

    @GameTest
    public void savingInvalidLoreChangesNothing(GameTestHelper helper) {
        final var player = holding(player(helper), withLore("Testing"));

        save(player, "Testing\n".repeat(LoreEditor.MAX_LINES + 1));

        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing"), "lore");
        helper.succeed();
    }

    @GameTest
    public void savingTooMuchTextChangesNothing(GameTestHelper helper) {
        final var player = holding(player(helper), withLore("Testing"));

        save(player, "a".repeat(LoreDialog.MAX_INPUT_LENGTH + 1));

        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing"), "lore");
        helper.succeed();
    }

    @GameTest
    public void spectatorsCantSave(GameTestHelper helper) {
        final var player = holding(player(helper), withLore("Testing"));
        player.setGameMode(GameType.SPECTATOR);

        save(player, "Changed");

        helper.assertValueEqual(LoreEditor.lines(player.getMainHandItem()), literals("Testing"), "lore");
        helper.succeed();
    }

    @GameTest
    public void dialogIsFilledWithTheTextCutDownToFit(GameTestHelper helper) {
        helper.assertValueEqual(initialText(LoreDialog.create("Testing", Optional.empty())), "Testing", "initial text");

        final var tooLong = "a".repeat(LoreDialog.MAX_INPUT_LENGTH + 1);
        helper.assertValueEqual(
            initialText(LoreDialog.create(tooLong, Optional.empty())).length(),
            LoreDialog.MAX_INPUT_LENGTH,
            "initial text length"
        );
        helper.succeed();
    }

    @GameTest
    public void dialogTextIsNotCutInsideAnEmoji(GameTestHelper helper) {
        // The emoji is two UTF-16 units, so the limit falls between them
        final var text = "a".repeat(LoreDialog.MAX_INPUT_LENGTH - 1) + "😀";

        helper.assertValueEqual(
            initialText(LoreDialog.create(text, Optional.empty())),
            "a".repeat(LoreDialog.MAX_INPUT_LENGTH - 1),
            "initial text"
        );
        helper.succeed();
    }

    private static String initialText(Dialog dialog) {
        return dialog.common().inputs().stream()
            .filter(input -> input.key().equals(LoreDialog.INPUT))
            .findFirst()
            .map(input -> (TextInput) input.control())
            .map(TextInput::initial)
            .orElseThrow();
    }

    private static ServerPlayer holding(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    // What the player's client sends when they press Save
    private static void save(ServerPlayer player, String input) {
        final var payload = new CompoundTag();
        payload.putString(LoreDialog.INPUT, input);
        player.connection.handleCustomClickAction(new ServerboundCustomClickActionPacket(
            LoreDialog.SAVE_ACTION,
            Optional.of(payload)
        ));
    }

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }
}
