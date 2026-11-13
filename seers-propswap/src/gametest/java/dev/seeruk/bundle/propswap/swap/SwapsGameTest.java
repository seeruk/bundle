package dev.seeruk.bundle.propswap.swap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;

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
        assertSwapped(helper, player(helper, Items.IRON_AXE), Blocks.OAK_LOG, state -> state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.succeed();
    }

    @GameTest
    public void swapsStonePillarAxisWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, player(helper, Items.IRON_PICKAXE), Blocks.QUARTZ_PILLAR, state -> state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.succeed();
    }

    @GameTest
    public void swapsWoodenStairShapeWithAxe(GameTestHelper helper) {
        assertSwapped(helper, player(helper, Items.IRON_AXE), Blocks.OAK_STAIRS, state -> state.setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT));
        helper.succeed();
    }

    @GameTest
    public void swapsStoneStairShapeWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, player(helper, Items.IRON_PICKAXE), Blocks.STONE_STAIRS, state -> state.setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT));
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
        assertSwapped(helper, player(helper, Items.IRON_AXE), Blocks.BROWN_MUSHROOM_BLOCK, state -> state.setValue(PipeBlock.UP, false));
        helper.succeed();
    }

    @GameTest
    public void swapsWoodenFenceConnectionsWithAxe(GameTestHelper helper) {
        assertSwapped(helper, crouching(player(helper, Items.IRON_AXE)), Blocks.OAK_FENCE, SwapsGameTest::connectAll);
        helper.succeed();
    }

    @GameTest
    public void swapsNetherBrickFenceConnectionsWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, crouching(player(helper, Items.IRON_PICKAXE)), Blocks.NETHER_BRICK_FENCE, SwapsGameTest::connectAll);
        helper.succeed();
    }

    @GameTest
    public void swapsIronBarConnectionsWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, crouching(player(helper, Items.IRON_PICKAXE)), Blocks.IRON_BARS, SwapsGameTest::connectAll);
        helper.succeed();
    }

    @GameTest
    public void swapsGlassPaneConnectionsWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, crouching(player(helper, Items.IRON_PICKAXE)), Blocks.GLASS_PANE, SwapsGameTest::connectAll);
        helper.succeed();
    }

    @GameTest
    public void swapsStainedGlassPaneConnectionsWithPickaxe(GameTestHelper helper) {
        final var pane = Blocks.STAINED_GLASS_PANE.pick(DyeColor.RED);
        assertSwapped(helper, crouching(player(helper, Items.IRON_PICKAXE)), pane, SwapsGameTest::connectAll);
        helper.succeed();
    }

    @GameTest
    public void swapsWallSidesWithPickaxe(GameTestHelper helper) {
        assertSwapped(helper, crouching(player(helper, Items.IRON_PICKAXE)), Blocks.COBBLESTONE_WALL, state -> {
            for (final var side : WallBlock.PROPERTY_BY_DIRECTION.values()) {
                state = state.setValue(side, WallSide.LOW);
            }
            return state;
        });
        helper.succeed();
    }

    private static ServerPlayer crouching(ServerPlayer player) {
        player.setShiftKeyDown(true);
        return player;
    }

    private static BlockState connectAll(BlockState state) {
        for (final var side : CrossCollisionBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(side, true);
        }
        return state;
    }

    private static void assertSwapped(GameTestHelper helper, ServerPlayer player, Block block, UnaryOperator<BlockState> expected) {
        helper.setBlock(POS, block);
        final var original = helper.getBlockState(POS);

        use(helper, player);

        helper.assertValueEqual(helper.getBlockState(POS), expected.apply(original), "block state");
    }
}
