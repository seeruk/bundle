package dev.seeruk.bundle.propswap.swap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.function.UnaryOperator;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.POS;
import static dev.seeruk.bundle.propswap.PropSwapFixtures.player;
import static dev.seeruk.bundle.propswap.PropSwapFixtures.use;

/**
 * Only checks each swap works in a real level with its default tool, the swaps' own tests cover
 * their behaviour.
 */
public class SwapsGameTest {
    @GameTest
    public void swapsLogAxisWithAxe(GameTestHelper helper) {
        assertSwapped(helper, Blocks.OAK_LOG, Items.IRON_AXE, state -> state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.succeed();
    }

    @GameTest
    public void swapsStonePillarAxisWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, Blocks.QUARTZ_PILLAR, Items.IRON_PICKAXE, state -> state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.succeed();
    }

    @GameTest
    public void swapsWoodenStairShapeWithAxe(GameTestHelper helper) {
        assertSwapped(helper, Blocks.OAK_STAIRS, Items.IRON_AXE, state -> state.setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT));
        helper.succeed();
    }

    @GameTest
    public void swapsStoneStairShapeWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, Blocks.STONE_STAIRS, Items.IRON_PICKAXE, state -> state.setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT));
        helper.succeed();
    }

    @GameTest
    public void swapsStairShapeBackwardsWhileCrouching(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.setShiftKeyDown(true);
        helper.setBlock(POS, Blocks.OAK_STAIRS);

        use(helper, player);

        final var expected = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT);
        helper.assertValueEqual(helper.getBlockState(POS), expected, "block state");
        helper.succeed();
    }

    @GameTest
    public void swapsMushroomFaceWithAxe(GameTestHelper helper) {
        // The fixture clicks the top of the block
        assertSwapped(helper, Blocks.BROWN_MUSHROOM_BLOCK, Items.IRON_AXE, state -> state.setValue(PipeBlock.UP, false));
        helper.succeed();
    }

    private static void assertSwapped(GameTestHelper helper, Block block, Item tool, UnaryOperator<BlockState> expected) {
        final var player = player(helper, tool);
        helper.setBlock(POS, block);
        final var original = helper.getBlockState(POS);

        use(helper, player);

        helper.assertValueEqual(helper.getBlockState(POS), expected.apply(original), "block state");
    }
}
