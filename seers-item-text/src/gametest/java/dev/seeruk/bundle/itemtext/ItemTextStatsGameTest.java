package dev.seeruk.bundle.itemtext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.itemtext.LoreFixtures.literals;
import static dev.seeruk.bundle.itemtext.LoreFixtures.withLore;
import static dev.seeruk.bundle.itemtext.NameFixtures.withName;

public class ItemTextStatsGameTest {
    private static final List<ScoreboardStat> STATS = List.of(
        ItemTextStats.NAME_WRITTEN,
        ItemTextStats.LORE_WRITTEN
    );

    @GameTest
    public void objectivesExistAfterServerStarts(GameTestHelper helper) {
        final var scoreboard = helper.getLevel().getServer().getScoreboard();

        for (final var stat : STATS) {
            helper.assertTrue(scoreboard.getObjective(stat.objectiveId()) != null, "Objective " + stat.objectiveId() + " doesn't exist");
        }

        helper.succeed();
    }

    @GameTest
    public void objectivesCanBeUsedInCommands(GameTestHelper helper) {
        // This might not be obvious. Malformed objective names can cause objective-related commands
        // to fail, while the objective itself can still exist.
        final var server = helper.getLevel().getServer();
        final var dispatcher = server.getCommands().getDispatcher();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        for (final var stat : STATS) {
            try {
                dispatcher.execute("scoreboard objectives setdisplay sidebar " + stat.objectiveId(), source);
                dispatcher.execute("scoreboard objectives setdisplay sidebar", source);
            } catch (CommandSyntaxException e) {
                helper.fail("Objective " + stat.objectiveId() + " couldn't be used in a command: " + e.getMessage());
            }
        }

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void settingNameAddsNameLength(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var before = score(player, ItemTextStats.NAME_WRITTEN);

        NameEditor.set(player, stack, "Testing");
        helper.assertValueEqual(score(player, ItemTextStats.NAME_WRITTEN) - before, 7, "score increase");

        NameEditor.set(player, stack, "Testing 2");
        helper.assertValueEqual(score(player, ItemTextStats.NAME_WRITTEN) - before, 16, "score increase");

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void tagsDoNotCountTowardsName(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var before = score(player, ItemTextStats.NAME_WRITTEN);

        NameEditor.set(player, new ItemStack(Items.NETHERITE_SWORD), "<red>Testing</red>");

        helper.assertValueEqual(score(player, ItemTextStats.NAME_WRITTEN) - before, 7, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void restyledNameDoesNotAddToScore(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");
        final var before = score(player, ItemTextStats.NAME_WRITTEN);

        NameEditor.set(player, stack, "<red>Testing</red>");

        helper.assertValueEqual(score(player, ItemTextStats.NAME_WRITTEN) - before, 0, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void unchangedOrResetNameDoesNotAddToScore(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");
        final var before = score(player, ItemTextStats.NAME_WRITTEN);

        NameEditor.set(player, stack, "Testing");
        NameEditor.reset(player, stack);

        helper.assertValueEqual(score(player, ItemTextStats.NAME_WRITTEN) - before, 0, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void addingLoreAddsLineLength(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1");
        final var before = score(player, ItemTextStats.LORE_WRITTEN);

        LoreEditor.add(player, stack, "Testing 2");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_WRITTEN) - before, 9, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void settingLoreOnlyCountsNewLine(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");
        final var before = score(player, ItemTextStats.LORE_WRITTEN);

        LoreEditor.set(player, stack, 2, "Test");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_WRITTEN) - before, 4, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void insertingLoreOnlyCountsNewLine(GameTestHelper helper) {
        // The existing lines are shifted down, but they shouldn't count again
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");
        final var before = score(player, ItemTextStats.LORE_WRITTEN);

        LoreEditor.insert(player, stack, 1, "Test");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_WRITTEN) - before, 4, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void duplicateLoreLinesStillCount(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing");
        final var before = score(player, ItemTextStats.LORE_WRITTEN);

        LoreEditor.add(player, stack, "Testing");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_WRITTEN) - before, 7, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removingOrClearingLoreDoesNotAddToScore(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2", "Testing 3");
        final var before = score(player, ItemTextStats.LORE_WRITTEN);

        LoreEditor.remove(player, stack, 1);
        LoreEditor.clear(player, stack, 1);
        LoreEditor.removeAll(player, stack);

        helper.assertValueEqual(score(player, ItemTextStats.LORE_WRITTEN) - before, 0, "score increase");
        helper.succeed();
    }
}
