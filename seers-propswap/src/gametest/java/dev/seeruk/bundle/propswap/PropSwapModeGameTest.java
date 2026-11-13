package dev.seeruk.bundle.propswap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.player;

public class PropSwapModeGameTest {
    @GameTest
    public void turnsItselfOffWhenIdle(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        final var now = helper.getLevel().getServer().getTickCount();

        PropSwapMode.expireIfIdle(player, now + PropSwapMode.IDLE_TICKS - 1);
        helper.assertTrue(PropSwapMode.isEnabled(player), "Turned off too early");

        PropSwapMode.expireIfIdle(player, now + PropSwapMode.IDLE_TICKS);
        helper.assertTrue(!PropSwapMode.isEnabled(player), "Didn't turn off");
        helper.succeed();
    }

    @GameTest
    public void usingPropSwapDoesNotEnableIt(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        PropSwapMode.disable(player);

        PropSwapMode.touch(player);

        helper.assertTrue(!PropSwapMode.isEnabled(player), "Using it enabled it");
        helper.succeed();
    }
}
