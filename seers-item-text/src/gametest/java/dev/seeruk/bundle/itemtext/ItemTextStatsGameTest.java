package dev.seeruk.bundle.itemtext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.itemtext.LoreFixtures.withLore;
import static dev.seeruk.bundle.itemtext.NameFixtures.withName;

public class ItemTextStatsGameTest {
    private static final List<ScoreboardStat> STATS = List.of(
        ItemTextStats.NAME_CHARACTERS,
        ItemTextStats.LORE_CHARACTERS,
        ItemTextStats.ITEMS_RENAMED,
        ItemTextStats.LORE_LINES_WRITTEN
    );

    @GameTest
    public void objectivesExistAfterServerStarts(GameTestHelper helper) {
        final var scoreboard = helper.getLevel().getServer().getScoreboard();

        for (final var stat : STATS) {
            helper.assertTrue(
                scoreboard.getObjective(stat.objectiveId()) != null,
                "Objective " + stat.objectiveId() + " doesn't exist"
            );
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
        final var before = score(player, ItemTextStats.NAME_CHARACTERS);

        NameEditor.set(player, stack, "Testing");
        helper.assertValueEqual(score(player, ItemTextStats.NAME_CHARACTERS) - before, 7, "score increase");

        NameEditor.set(player, stack, "Testing 2");
        helper.assertValueEqual(score(player, ItemTextStats.NAME_CHARACTERS) - before, 16, "score increase");

        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void tagsDoNotCountTowardsName(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var before = score(player, ItemTextStats.NAME_CHARACTERS);

        NameEditor.set(player, new ItemStack(Items.NETHERITE_SWORD), "<red>Testing</red>");

        helper.assertValueEqual(score(player, ItemTextStats.NAME_CHARACTERS) - before, 7, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void restyledNameDoesNotAddToScore(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");
        final var before = score(player, ItemTextStats.NAME_CHARACTERS);

        NameEditor.set(player, stack, "<red>Testing</red>");

        helper.assertValueEqual(score(player, ItemTextStats.NAME_CHARACTERS) - before, 0, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void unchangedOrResetNameDoesNotAddToScore(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");
        final var before = score(player, ItemTextStats.NAME_CHARACTERS);

        NameEditor.set(player, stack, "Testing");
        NameEditor.reset(player, stack);

        helper.assertValueEqual(score(player, ItemTextStats.NAME_CHARACTERS) - before, 0, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void addingLoreAddsNewWordsLength(GameTestHelper helper) {
        // "Testing" is new as well as "2", as the first line uses the original's only "Testing"
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1");
        final var before = score(player, ItemTextStats.LORE_CHARACTERS);

        LoreEditor.replace(player, stack, "Testing 1\nTesting 2");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_CHARACTERS) - before, 8, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void rewrappedLoreOnlyCountsNewWords(GameTestHelper helper) {
        // Adding a word to the start of a long line moves where every wrapped line breaks
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var paragraph = "word ".repeat(30).strip();
        LoreEditor.replace(player, stack, paragraph);
        final var characters = score(player, ItemTextStats.LORE_CHARACTERS);
        final var lines = score(player, ItemTextStats.LORE_LINES_WRITTEN);

        LoreEditor.replace(player, stack, "New " + paragraph);

        helper.assertValueEqual(
            score(player, ItemTextStats.LORE_CHARACTERS) - characters,
            3,
            "characters score increase"
        );
        helper.assertValueEqual(score(player, ItemTextStats.LORE_LINES_WRITTEN) - lines, 1, "lines score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void changingALineOnlyCountsThatLine(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");
        final var before = score(player, ItemTextStats.LORE_CHARACTERS);

        LoreEditor.replace(player, stack, "Testing 1\nTest");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_CHARACTERS) - before, 4, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void movedLinesDoNotCount(GameTestHelper helper) {
        // The existing lines are moved down, but they shouldn't count again
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");
        final var before = score(player, ItemTextStats.LORE_CHARACTERS);

        LoreEditor.replace(player, stack, "Test\nTesting 1\nTesting 2");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_CHARACTERS) - before, 4, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void duplicateLoreLinesStillCount(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing");
        final var before = score(player, ItemTextStats.LORE_CHARACTERS);

        LoreEditor.replace(player, stack, "Testing\nTesting");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_CHARACTERS) - before, 7, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removingOrClearingLoreDoesNotAddToScore(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2", "Testing 3");
        final var before = score(player, ItemTextStats.LORE_CHARACTERS);

        LoreEditor.replace(player, stack, "\nTesting 3");
        LoreEditor.remove(player, stack);

        helper.assertValueEqual(score(player, ItemTextStats.LORE_CHARACTERS) - before, 0, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void renamingAddsOnePerName(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = new ItemStack(Items.NETHERITE_SWORD);
        final var before = score(player, ItemTextStats.ITEMS_RENAMED);

        NameEditor.set(player, stack, "Testing");
        NameEditor.set(player, stack, "Testing 2");

        helper.assertValueEqual(score(player, ItemTextStats.ITEMS_RENAMED) - before, 2, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void restyledUnchangedOrResetNameDoesNotCountAsRenamed(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withName("Testing");
        final var before = score(player, ItemTextStats.ITEMS_RENAMED);

        NameEditor.set(player, stack, "<red>Testing</red>");
        NameEditor.set(player, stack, "<red>Testing</red>");
        NameEditor.reset(player, stack);

        helper.assertValueEqual(score(player, ItemTextStats.ITEMS_RENAMED) - before, 0, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void addingLoreAddsOneLine(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1");
        final var before = score(player, ItemTextStats.LORE_LINES_WRITTEN);

        LoreEditor.replace(player, stack, "Testing 1\nTesting 2");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_LINES_WRITTEN) - before, 1, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void movedAndBlankLinesDoNotCountAsWritten(GameTestHelper helper) {
        // The existing lines are moved down, and blank lines are added, but only the lines with
        // text in them were written
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2");
        final var before = score(player, ItemTextStats.LORE_LINES_WRITTEN);

        LoreEditor.replace(player, stack, "Test\nTesting 1\nTesting 2\n\n\nTest 2");

        helper.assertValueEqual(score(player, ItemTextStats.LORE_LINES_WRITTEN) - before, 2, "score increase");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void removingOrClearingLoreDoesNotAddLines(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var stack = withLore("Testing 1", "Testing 2", "Testing 3");
        final var before = score(player, ItemTextStats.LORE_LINES_WRITTEN);

        LoreEditor.replace(player, stack, "\nTesting 3");
        LoreEditor.remove(player, stack);

        helper.assertValueEqual(score(player, ItemTextStats.LORE_LINES_WRITTEN) - before, 0, "score increase");
        helper.succeed();
    }
}
