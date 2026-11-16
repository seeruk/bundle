package dev.seeruk.bundle.propswap;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.propswap.PropSwapFixtures.*;

public class PropSwapStatsGameTest {
    private static final String OBJECTIVE_ID = PropSwapStats.SWAPPED.objectiveId();

    @GameTest
    public void objectiveExistsAfterServerStarts(GameTestHelper helper) {
        final var objective = helper.getLevel().getServer().getScoreboard().getObjective(OBJECTIVE_ID);

        helper.assertTrue(objective != null, "Objective " + OBJECTIVE_ID + " doesn't exist");
        helper.succeed();
    }

    @GameTest
    public void objectiveCanBeUsedInCommands(GameTestHelper helper) {
        // This might not be obvious. Malformed objective names can cause objective-related commands
        // to fail, while the objective itself can still exist.
        final var server = helper.getLevel().getServer();
        final var dispatcher = server.getCommands().getDispatcher();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            dispatcher.execute("scoreboard objectives setdisplay sidebar " + OBJECTIVE_ID, source);
            dispatcher.execute("scoreboard objectives setdisplay sidebar", source);
        } catch (CommandSyntaxException e) {
            helper.fail("Objective couldn't be used in a command: " + e.getMessage());
        }

        helper.succeed();
    }

    @GameTest
    public void swappingAddsOnePerBlock(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        helper.setBlock(POS, Blocks.FURNACE);
        final var before = score(player, PropSwapStats.SWAPPED);

        use(helper, player);
        use(helper, player);

        helper.assertValueEqual(score(player, PropSwapStats.SWAPPED) - before, 2, "score increase");
        helper.succeed();
    }

    @GameTest
    public void ignoredInteractionDoesNotAddToScore(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        helper.setBlock(POS, Blocks.FURNACE);
        final var before = score(player, PropSwapStats.SWAPPED);

        use(helper, player);

        helper.assertValueEqual(score(player, PropSwapStats.SWAPPED) - before, 0, "score increase");
        helper.succeed();
    }
}
