package dev.seeruk.bundle.disposal;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.seeruk.bundle.core.testing.StatScores.score;

public class DisposalStatsGameTest {
    private static final String OBJECTIVE_ID = DisposalStats.DISPOSED.objectiveId();

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

    @SuppressWarnings("removal")
    @GameTest
    public void disposingAddsTotalItemCount(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var before = score(player, DisposalStats.DISPOSED);

        dispose(player, new ItemStack(Items.DIRT, 16), new ItemStack(Items.DIAMOND, 3));
        helper.assertValueEqual(score(player, DisposalStats.DISPOSED) - before, 19, "score increase");

        dispose(player, new ItemStack(Items.DIRT, 7));
        helper.assertValueEqual(score(player, DisposalStats.DISPOSED) - before, 26, "score increase");

        helper.succeed();
    }

    private static void dispose(ServerPlayer player, ItemStack... stacks) {
        final var container = new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS);
        for (var i = 0; i < stacks.length; i++) {
            container.setItem(i, stacks[i]);
        }

        player.openMenu(new SimpleMenuProvider(
            (id, inventory, owner) -> new DisposalMenu(id, inventory, container),
            Component.literal("Disposal")
        ));
        player.closeContainer();
    }
}
