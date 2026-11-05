package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.itemtext.api.ItemLoreEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.Arrays;
import java.util.List;

public class LoreEditorGameTest {
    private static final EventLog<Edited> EVENTS = new EventLog<>();

    static {
        ItemLoreEvents.EDITED.register((player, item, original, edited) ->
            EVENTS.record(new Edited(player, item, original, edited)));
    }

    @SuppressWarnings("removal")
    @GameTest
    public void addAppendsLineAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1");

        LoreEditor.add(player, stack, "Testing 2");

        final var expected = literals("Testing 1", "Testing 2");
        helper.assertValueEqual(LoreEditor.lines(stack), expected, "lore");
        helper.assertValueEqual(eventsFor(player), List.of(new Edited(player, stack, literals("Testing 1"), expected)), "events");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setReplacesLineAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        LoreEditor.set(player, stack, 2, "Testing 3");

        final var expected = literals("Testing 1", "Testing 3");
        helper.assertValueEqual(LoreEditor.lines(stack), expected, "lore");
        helper.assertValueEqual(eventsFor(player), List.of(new Edited(player, stack, literals("Testing 1", "Testing 2"), expected)), "events");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void insertInsertsLineAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        LoreEditor.insert(player, stack, 1, "Testing 3");

        final var expected = literals("Testing 3", "Testing 1", "Testing 2");
        helper.assertValueEqual(LoreEditor.lines(stack), expected, "lore");
        helper.assertValueEqual(eventsFor(player), List.of(new Edited(player, stack, literals("Testing 1", "Testing 2"), expected)), "events");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeRemovesLineAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        LoreEditor.remove(player, stack, 1);

        final var expected = literals("Testing 2");
        helper.assertValueEqual(LoreEditor.lines(stack), expected, "lore");
        helper.assertValueEqual(eventsFor(player), List.of(new Edited(player, stack, literals("Testing 1", "Testing 2"), expected)), "events");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeAllRemovesLoreAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        LoreEditor.removeAll(player, stack);

        helper.assertValueEqual(LoreEditor.lines(stack), List.of(), "lore");
        helper.assertValueEqual(eventsFor(player), List.of(new Edited(player, stack, literals("Testing 1", "Testing 2"), List.of())), "events");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void clearEmptiesLineAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        LoreEditor.clear(player, stack, 1);

        final List<Component> expected = List.of(Component.empty(), Component.literal("Testing 2"));
        helper.assertValueEqual(LoreEditor.lines(stack), expected, "lore");
        helper.assertValueEqual(eventsFor(player), List.of(new Edited(player, stack, literals("Testing 1", "Testing 2"), expected)), "events");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void parsesSafeTags(GameTestHelper helper) {
        // Only checks which parser is used, TextParsersTest covers which tags are safe
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);

        LoreEditor.add(player, stack, "<red>Testing</red>");

        helper.assertValueEqual(LoreEditor.lines(stack), List.of(Component.literal("Testing").withStyle(ChatFormatting.RED)), "lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void failedEditDoesNotModifyItemLoreOrFireEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        final var result = LoreEditor.set(player, stack, 3, "Testing 3");

        helper.assertTrue(result instanceof LoreEditResult.Failure, "Edit didn't fail");
        helper.assertValueEqual(LoreEditor.lines(stack), literals("Testing 1", "Testing 2"), "lore");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for a failed edit");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void doesNotParseUnsafeTags(GameTestHelper helper) {
        // Only checks which parser is used, TextParsersTest covers which tags are unsafe
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var input = "<run_cmd '/say hi'>Testing</run_cmd>";

        LoreEditor.add(player, stack, input);

        helper.assertValueEqual(LoreEditor.lines(stack), literals(input), "lore");
        helper.succeed();
    }

    private record Edited(Player player, ItemStack stack, List<Component> original, List<Component> edited) {}

    private static List<Edited> eventsFor(Player player) {
        return EVENTS.matching(event -> event.player() == player);
    }

    private static ItemStack withLore(String... lines) {
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        stack.set(DataComponents.LORE, new ItemLore(literals(lines)));
        return stack;
    }

    private static List<Component> literals(String... lines) {
        return Arrays.stream(lines).<Component>map(Component::literal).toList();
    }
}
