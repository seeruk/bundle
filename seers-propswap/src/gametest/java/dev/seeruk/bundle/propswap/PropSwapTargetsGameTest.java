package dev.seeruk.bundle.propswap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.POS;
import static dev.seeruk.bundle.propswap.PropSwapFixtures.player;

public class PropSwapTargetsGameTest {
    @GameTest
    public void supportsRightToolOnSupportedBlock(GameTestHelper helper) {
        helper.assertTrue(
            PropSwapTargets.supports(new ItemStack(Items.IRON_AXE), Blocks.OAK_LOG.defaultBlockState()),
            "Axe on a log wasn't supported"
        );
        helper.assertTrue(
            PropSwapTargets.supports(new ItemStack(Items.IRON_PICKAXE), Blocks.COBBLESTONE_WALL.defaultBlockState()),
            "Pickaxe on a wall wasn't supported"
        );
        helper.succeed();
    }

    @GameTest
    public void ignoresWrongToolOrUnsupportedBlock(GameTestHelper helper) {
        // Dirt can be dug with a shovel, but there's nothing for PropSwap to change
        helper.assertTrue(
            !PropSwapTargets.supports(new ItemStack(Items.IRON_SHOVEL), Blocks.DIRT.defaultBlockState()),
            "Shovel on dirt was supported"
        );
        helper.assertTrue(
            !PropSwapTargets.supports(new ItemStack(Items.IRON_PICKAXE), Blocks.OAK_LOG.defaultBlockState()),
            "Pickaxe on a log was supported"
        );
        helper.assertTrue(
            !PropSwapTargets.supports(new ItemStack(Items.STICK), Blocks.OAK_LOG.defaultBlockState()),
            "Stick on a log was supported"
        );
        helper.succeed();
    }

    @GameTest
    public void itemThatCountsAsSeveralToolsWorksAsEach(GameTestHelper helper) {
        // The GameTest data pack tags tridents as both axes and pickaxes, like a modded paxel
        final var trident = new ItemStack(Items.TRIDENT);

        helper.assertTrue(
            PropSwapTargets.supports(trident, Blocks.OAK_LOG.defaultBlockState()),
            "Didn't work as an axe"
        );
        helper.assertTrue(
            PropSwapTargets.supports(trident, Blocks.COBBLESTONE_WALL.defaultBlockState()),
            "Didn't work as a pickaxe"
        );
        helper.succeed();
    }

    @GameTest
    public void playersWhoCannotBuildAreNotAllowed(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        final var pos = helper.absolutePos(POS);
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        final var stack = player.getMainHandItem();
        final var state = Blocks.OAK_LOG.defaultBlockState();

        helper.assertTrue(PropSwapTargets.allowed(player, stack, state, hit), "Survival player wasn't allowed");

        player.setGameMode(GameType.ADVENTURE);
        helper.assertTrue(!PropSwapTargets.allowed(player, stack, state, hit), "Adventure player was allowed");
        helper.succeed();
    }
}
