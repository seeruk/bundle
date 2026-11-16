package dev.seeruk.bundle.propswap.swap;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.*;

public class BarrelSwapGameTest {
    @GameTest
    public void rotatesBarrelWithAxe(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        helper.setBlock(POS, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));

        use(helper, player, Direction.NORTH);

        helper.assertValueEqual(helper.getBlockState(POS).getValue(BarrelBlock.FACING), Direction.NORTH, "facing");
        helper.assertTrue(!isLocked(helper), "Rotated barrel was locked");
        helper.succeed();
    }

    @GameTest
    public void crouchingForcesBarrelOpen(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.setShiftKeyDown(true);
        helper.setBlock(POS, Blocks.BARREL);

        use(helper, player);

        // Opening and closing it would normally leave it closed
        final var barrel = (BarrelBlockEntity) helper.getBlockEntity(POS, BarrelBlockEntity.class);
        barrel.startOpen(player);
        barrel.stopOpen(player);

        helper.assertValueEqual(helper.getBlockState(POS).getValue(BarrelBlock.OPEN), true, "open");
        helper.assertTrue(isLocked(helper), "Forced open barrel wasn't locked");
        helper.succeed();
    }

    @GameTest
    public void crouchingAgainStopsForcingBarrelOpen(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.setShiftKeyDown(true);
        helper.setBlock(POS, Blocks.BARREL);

        use(helper, player);
        use(helper, player);

        helper.assertValueEqual(helper.getBlockState(POS).getValue(BarrelBlock.OPEN), false, "open");
        helper.assertTrue(!isLocked(helper), "Barrel was still locked");
        helper.succeed();
    }

    private static boolean isLocked(GameTestHelper helper) {
        return LockedBlocks.isLocked(helper.getLevel(), helper.absolutePos(POS));
    }
}
