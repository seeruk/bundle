package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.itemtext.api.ItemNameEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.itemtext.NameFixtures.literal;
import static dev.seeruk.bundle.itemtext.NameFixtures.withName;

public class NameEditorGameTest {
    private static final EventLog<Changed> EVENTS = new EventLog<>();

    static {
        ItemNameEvents.CHANGED.register((player, item, original, edited) ->
            EVENTS.record(new Changed(player, item, original, edited)));
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setAddsNameAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);

        NameEditor.set(player, stack, "Testing");

        final var expected = literal("Testing");
        helper.assertValueEqual(NameEditor.customName(stack), expected, "name");
        helper.assertValueEqual(
            eventsFor(player),
            List.of(new Changed(player, stack, Optional.empty(), expected)),
            "events"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setReplacesNameAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing 1");

        NameEditor.set(player, stack, "Testing 2");

        final var expected = literal("Testing 2");
        helper.assertValueEqual(NameEditor.customName(stack), expected, "name");
        helper.assertValueEqual(
            eventsFor(player),
            List.of(new Changed(player, stack, literal("Testing 1"), expected)),
            "events"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setWithSameNameSucceedsWithoutFiringEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");

        final var result = NameEditor.set(player, stack, "Testing");

        helper.assertTrue(result instanceof NameEditResult.Edited, "Edit didn't succeed");
        helper.assertValueEqual(NameEditor.customName(stack), literal("Testing"), "name");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for an unchanged name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setAcceptsNameAtMaxLength(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var input = "a".repeat(AnvilMenu.MAX_NAME_LENGTH);

        final var result = NameEditor.set(player, stack, input);

        helper.assertTrue(result instanceof NameEditResult.Edited, "Edit didn't succeed");
        helper.assertValueEqual(NameEditor.customName(stack), literal(input), "name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setCountsMaxLengthOfPlainText(GameTestHelper helper) {
        // The tags push the raw input over the limit, but the visible text is still within it
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var name = "a".repeat(AnvilMenu.MAX_NAME_LENGTH);

        final var result = NameEditor.set(player, stack, "<red>" + name + "</red>");

        helper.assertTrue(result instanceof NameEditResult.Edited, "Edit didn't succeed");
        helper.assertValueEqual(
            NameEditor.customName(stack),
            Optional.<Component>of(Component.literal(name).withStyle(ChatFormatting.RED)),
            "name"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void resetRemovesNameAndFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");

        NameEditor.reset(player, stack);

        helper.assertValueEqual(NameEditor.customName(stack), Optional.empty(), "name");
        helper.assertValueEqual(
            eventsFor(player),
            List.of(new Changed(player, stack, literal("Testing"), Optional.empty())),
            "events"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void resetRemovesBlankNameAndFiresEventWithoutOriginal(GameTestHelper helper) {
        // Other mods may leave blank names on items, these should still be removable, but they
        // shouldn't be presented as a name in events
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName(" ");

        NameEditor.reset(player, stack);

        helper.assertValueEqual(NameEditor.customName(stack), Optional.empty(), "name");
        helper.assertValueEqual(
            eventsFor(player),
            List.of(new Changed(player, stack, Optional.empty(), Optional.empty())),
            "events"
        );
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void parsesSafeTags(GameTestHelper helper) {
        // Only checks which parser is used, TextParsersTest covers which tags are safe
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);

        NameEditor.set(player, stack, "<red>Testing</red>");

        helper.assertValueEqual(
            NameEditor.customName(stack),
            Optional.<Component>of(Component.literal("Testing").withStyle(ChatFormatting.RED)),
            "name"
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

        NameEditor.set(player, stack, input);

        helper.assertValueEqual(NameEditor.customName(stack), literal(input), "name");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setFailsForBlankName(GameTestHelper helper) {
        // The tags aren't blank, but the visible text is
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");

        final var result = NameEditor.set(player, stack, "<red> </red>");

        helper.assertTrue(result instanceof NameEditResult.BlankName, "Edit didn't fail with BlankName");
        helper.assertValueEqual(NameEditor.customName(stack), literal("Testing"), "name");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for a failed edit");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void setFailsForNameOverMaxLength(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");

        final var result = NameEditor.set(player, stack, "a".repeat(AnvilMenu.MAX_NAME_LENGTH + 1));

        helper.assertTrue(
            result instanceof NameEditResult.MaxLengthExceeded,
            "Edit didn't fail with MaxLengthExceeded"
        );
        helper.assertValueEqual(NameEditor.customName(stack), literal("Testing"), "name");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for a failed edit");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void resetFailsWithoutName(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);

        final var result = NameEditor.reset(player, stack);

        helper.assertTrue(result instanceof NameEditResult.NoCustomName, "Edit didn't fail with NoCustomName");
        helper.assertValueEqual(NameEditor.customName(stack), Optional.empty(), "name");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for a failed edit");
        helper.succeed();
    }

    private record Changed(Player player, ItemStack stack, Optional<Component> original, Optional<Component> edited) {}

    private static List<Changed> eventsFor(Player player) {
        return EVENTS.matching(event -> event.player() == player);
    }
}
