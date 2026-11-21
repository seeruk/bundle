package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.itemtext.api.ItemLoreEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.itemtext.LoreFixtures.literals;
import static dev.seeruk.bundle.itemtext.LoreFixtures.withLore;

public class LoreEditorGameTest {
    private static final EventLog<Changed> EVENTS = new EventLog<>();

    static {
        ItemLoreEvents.CHANGED.register((player, item, original, edited) ->
            EVENTS.record(new Changed(player, item, original, edited)));
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeRemovesLoreAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");

        LoreEditor.remove(player, stack);

        helper.assertValueEqual(LoreEditor.lines(stack), List.of(), "lore");
        helper.assertValueEqual(
            eventsFor(player),
            List.of(new Changed(player, stack, literals("Testing 1", "Testing 2"), List.of())),
            "events"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removeWithoutLoreSucceedsWithoutFiringEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);

        final var result = LoreEditor.remove(player, stack);

        helper.assertTrue(result instanceof LoreEditResult.Edited, "Edit didn't succeed");
        helper.assertValueEqual(LoreEditor.lines(stack), List.of(), "lore");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for unchanged lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removingOrReplacingWithNothingForgetsWhatWasWritten(GameTestHelper helper) {
        // Leaving the text behind would stop the item stacking with others like it
        final var player = helper.makeMockServerPlayerInLevel();
        final var removed = new ItemStack(Items.NETHERITE_SWORD);
        final var replaced = new ItemStack(Items.NETHERITE_SWORD);
        LoreEditor.replace(player, removed, "Testing");
        LoreEditor.replace(player, replaced, "Testing");

        LoreEditor.remove(player, removed);
        LoreEditor.replace(player, replaced, "");

        helper.assertFalse(removed.has(DataComponents.CUSTOM_DATA), "Removed lore left custom data");
        helper.assertFalse(replaced.has(DataComponents.CUSTOM_DATA), "Lore replaced with nothing left custom data");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void parsesSafeTags(GameTestHelper helper) {
        // Only checks which parser is used, TextParsersTest covers which tags are safe
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);

        LoreEditor.replace(player, stack, "<red>Testing</red>");

        final var line = LoreEditor.lines(stack).getFirst();
        helper.assertValueEqual(line.getString(), "Testing", "lore");
        helper.assertValueEqual(
            line.toFlatList().getFirst().getStyle().getColor(),
            TextColor.fromLegacyFormat(ChatFormatting.RED),
            "colour"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void doesNotParseUnsafeTags(GameTestHelper helper) {
        // Only checks which parser is used, TextParsersTest covers which tags are unsafe
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var input = "<run_cmd '/say hi'>Testing</run_cmd>";

        LoreEditor.replace(player, stack, input);

        helper.assertValueEqual(plain(LoreEditor.lines(stack)), List.of(input), "lore");
        helper.assertTrue(
            LoreEditor.lines(stack).getFirst().toFlatList().stream().allMatch(part -> part.getStyle().getClickEvent() == null),
            "Lore had a click event"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void replaceSetsEveryLineAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1");

        LoreEditor.replace(player, stack, "Testing 2\nTesting 3");

        final var expected = literals("Testing 2", "Testing 3");
        helper.assertValueEqual(plain(LoreEditor.lines(stack)), List.of("Testing 2", "Testing 3"), "lore");
        helper.assertValueEqual(eventsFor(player).size(), 1, "event count");
        helper.assertValueEqual(plain(eventsFor(player).getFirst().edited()), plain(expected), "event's edited lore");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void replaceWithSameLoreSucceedsWithoutFiringEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        LoreEditor.replace(player, stack, "<red>Testing 1\nTesting 2</red>");
        final var mark = EVENTS.mark();

        final var result = LoreEditor.replace(player, stack, "<red>Testing 1\nTesting 2</red>");

        helper.assertTrue(result instanceof LoreEditResult.Edited, "Edit didn't succeed");
        helper.assertTrue(
            EVENTS.matchingSince(mark, event -> event.player() == player).isEmpty(),
            "Event fired for unchanged lore"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void replaceWithTooManyLinesChangesNothing(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing");

        final var result = LoreEditor.replace(player, stack, "Testing\n".repeat(LoreEditor.MAX_LINES + 1));

        helper.assertTrue(result instanceof LoreEditResult.MaxLinesExceeded, "Edit didn't fail with MaxLinesExceeded");
        helper.assertValueEqual(LoreEditor.lines(stack), literals("Testing"), "lore");
        helper.assertTrue(LoreInput.get(stack).isEmpty(), "Input was remembered for a failed edit");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for a failed edit");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void replaceRemembersWhatWasWritten(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var input = "<red>Testing 1\nTesting 2</red>";

        LoreEditor.replace(player, stack, input);

        helper.assertValueEqual(LoreInput.get(stack), Optional.of(input), "remembered input");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void rememberedInputIsIgnoredOnceLoreChangesAnotherWay(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        LoreEditor.replace(player, stack, "<red>Testing 1</red>");

        // e.g. by another mod
        stack.set(DataComponents.LORE, new ItemLore(literals("Testing 2")));

        helper.assertTrue(LoreInput.get(stack).isEmpty(), "Remembered input didn't match the lore");
        helper.succeed();
    }

    private record Changed(Player player, ItemStack stack, List<Component> original, List<Component> edited) {}

    private static List<Changed> eventsFor(Player player) {
        return EVENTS.matching(event -> event.player() == player);
    }

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(Component::getString).toList();
    }
}
