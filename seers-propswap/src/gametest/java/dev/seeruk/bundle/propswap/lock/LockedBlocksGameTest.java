package dev.seeruk.bundle.propswap.lock;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.List;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.*;

public class LockedBlocksGameTest {
    @GameTest
    public void lockedStairKeepsShapeWhenNeighbourChanges(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        helper.setBlock(POS, Blocks.STONE_STAIRS);

        use(helper, player);
        // Another stair next to it would normally make it recalculate its shape
        helper.setBlock(POS.east(), Blocks.STONE_STAIRS);

        helper.assertValueEqual(helper.getBlockState(POS).getValue(StairBlock.SHAPE), StairsShape.INNER_LEFT, "shape");
        helper.succeed();
    }

    @GameTest
    public void lockedFenceKeepsConnectionWhenNeighbourChanges(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        helper.setBlock(POS, Blocks.OAK_FENCE);

        use(helper, player, Direction.NORTH);
        // Fences don't connect to torches, so this would normally drop the connection
        helper.setBlock(POS.north(), Blocks.TORCH);

        helper.assertValueEqual(
            helper.getBlockState(POS).getValue(CrossCollisionBlock.NORTH),
            true,
            "north connection"
        );
        helper.succeed();
    }

    @GameTest
    public void lockedWaterloggedBlockStillLetsWaterFlow(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        for (final var side : List.of(
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.DOWN
        )) {
            helper.setBlock(POS.relative(side), Blocks.STONE);
        }
        helper.setBlock(POS, Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        use(helper, player, Direction.NORTH);

        // Opening up a side should let the water out, even though the fence keeps its shape
        helper.setBlock(POS.east(), Blocks.AIR);

        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER, POS.east()));
    }

    @GameTest
    public void lockClearsWhenBlockIsReplaced(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        helper.setBlock(POS, Blocks.OAK_FENCE);
        use(helper, player, Direction.NORTH);

        helper.setBlock(POS, Blocks.AIR);
        helper.assertTrue(!isLocked(helper), "Lock wasn't cleared");

        helper.setBlock(POS, Blocks.OAK_FENCE);
        helper.setBlock(POS.east(), Blocks.STONE);
        helper.assertValueEqual(helper.getBlockState(POS).getValue(CrossCollisionBlock.EAST), true, "east connection");
        helper.succeed();
    }

    @GameTest
    public void swapsThatDoNotNeedLockingDoNotLock(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        helper.setBlock(POS, Blocks.FURNACE);

        use(helper, player);

        helper.assertTrue(!isLocked(helper), "Furnace was locked");
        helper.succeed();
    }

    @GameTest
    public void refusedSwapDoesNotLock(GameTestHelper helper) {
        // Crouching on a side toggles the post, but a wall with only a post can't lose it
        final var player = player(helper, Items.IRON_PICKAXE);
        player.setShiftKeyDown(true);
        helper.setBlock(POS, Blocks.COBBLESTONE_WALL);

        use(helper, player, Direction.NORTH);

        helper.assertTrue(!isLocked(helper), "Wall was locked");
        helper.succeed();
    }

    private static boolean isLocked(GameTestHelper helper) {
        return LockedBlocks.isLocked(helper.getLevel(), helper.absolutePos(POS));
    }
}
