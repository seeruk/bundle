package dev.seeruk.bundle.propswap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.player;

public class PropSwapReminderGameTest {
    @GameTest
    public void remindsWhileEnabledAndHoldingATool(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);

        helper.assertTrue(PropSwapReminder.message(player).isPresent(), "No reminder while holding a tool");
        helper.succeed();
    }

    @GameTest
    public void remindsWithAToolInTheOffHand(GameTestHelper helper) {
        final var player = player(helper, Items.STICK);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_SHOVEL));

        helper.assertTrue(PropSwapReminder.message(player).isPresent(), "No reminder with a tool in the off hand");
        helper.succeed();
    }

    @GameTest
    public void onlyRemindsWhileEnabledAndHoldingATool(GameTestHelper helper) {
        final var player = player(helper, Items.STICK);
        helper.assertTrue(PropSwapReminder.message(player).isEmpty(), "Reminded without a tool");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        PropSwapMode.disable(player);

        helper.assertTrue(PropSwapReminder.message(player).isEmpty(), "Reminded while disabled");
        helper.succeed();
    }
}
