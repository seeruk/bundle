package dev.seeruk.bundle.propswap.swap;

import dev.seeruk.bundle.propswap.lock.LockedBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.*;

/**
 * Most of these are about making sure beds can't be duplicated (or lost) by splitting them. The
 * fixture's position is always the foot, with the head north of it.
 */
public class BedSwapGameTest {
    private static final Item RED_BED = Items.BED.pick(DyeColor.RED);
    private static final Item BLUE_BED = Items.BED.pick(DyeColor.BLUE);
    private static final BlockPos HEAD = POS.north();

    @GameTest
    public void splittingCostsABedOfTheSameColour(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.getInventory().add(new ItemStack(BLUE_BED));
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));

        helper.assertValueEqual(use(helper, player), InteractionResult.SUCCESS, "result without a red bed");
        helper.assertTrue(!isLocked(helper, POS), "Split without a red bed");

        player.getInventory().add(new ItemStack(RED_BED));
        use(helper, player);

        helper.assertTrue(isLocked(helper, POS) && isLocked(helper, HEAD), "Both halves weren't locked");
        helper.assertValueEqual(count(player, RED_BED), 0, "red beds after split");
        helper.assertValueEqual(count(player, BLUE_BED), 1, "blue beds after split");
        helper.succeed();
    }

    @GameTest
    public void splittingIsFreeInCreative(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.setGameMode(GameType.CREATIVE);
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));

        use(helper, player);

        helper.assertTrue(isLocked(helper, POS) && isLocked(helper, HEAD), "Both halves weren't locked");
        helper.succeed();
    }

    @GameTest
    public void flippingSplitHalvesIsFree(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.getInventory().add(new ItemStack(RED_BED, 2));
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));

        use(helper, player);
        use(helper, player);

        helper.assertValueEqual(helper.getBlockState(POS).getValue(AbstractBedBlock.PART), BedPart.HEAD, "part");
        helper.assertValueEqual(helper.getBlockState(POS).getValue(AbstractBedBlock.FACING), Direction.SOUTH, "facing");
        helper.assertValueEqual(count(player, RED_BED), 1, "red beds after flip");
        helper.succeed();
    }

    @GameTest
    public void splitHalvesDropABedEach(GameTestHelper helper) {
        // One bed placed, plus one paid to split it, so two should come back
        final var player = player(helper, Items.IRON_AXE);
        player.getInventory().add(new ItemStack(RED_BED));
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));
        use(helper, player);

        destroy(helper, player, POS);
        helper.assertBlockPresent(Blocks.BED.pick(DyeColor.RED), HEAD);
        helper.assertItemEntityCountIs(RED_BED, POS, 3.0, 1);

        destroy(helper, player, HEAD);
        helper.assertItemEntityCountIs(RED_BED, POS, 3.0, 2);
        helper.succeed();
    }

    @GameTest
    public void unsplitBedDropsOneBed(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));

        destroy(helper, player, POS);

        helper.assertBlockPresent(Blocks.AIR, HEAD);
        helper.assertItemEntityCountIs(RED_BED, POS, 3.0, 1);
        helper.succeed();
    }

    @GameTest
    public void breakingSplitFootInCreativeKeepsHead(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.setGameMode(GameType.CREATIVE);
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));
        use(helper, player);

        destroy(helper, player, POS);

        helper.assertBlockPresent(Blocks.BED.pick(DyeColor.RED), HEAD);
        helper.assertItemEntityNotPresent(RED_BED, POS, 3.0);
        helper.succeed();
    }

    @GameTest
    public void newBedInSameTickIsNotTreatedAsSplit(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        player.getInventory().add(new ItemStack(RED_BED));
        placeBed(helper, Blocks.BED.pick(DyeColor.RED));
        use(helper, player);
        destroy(helper, player, POS);

        // A normal bed with its foot in the same spot, before the tick ends
        final var blueBed = Blocks.BED.pick(DyeColor.BLUE);
        helper.setBlock(
            POS.west(), blueBed.defaultBlockState()
                .setValue(AbstractBedBlock.PART, BedPart.HEAD)
                .setValue(AbstractBedBlock.FACING, Direction.WEST)
        );
        helper.setBlock(
            POS, blueBed.defaultBlockState()
                .setValue(AbstractBedBlock.PART, BedPart.FOOT)
                .setValue(AbstractBedBlock.FACING, Direction.WEST)
        );

        // Work out the foot's drops on their own, as removing it would break the head too
        Block.dropResources(helper.getBlockState(POS), helper.getLevel(), helper.absolutePos(POS));

        helper.assertItemEntityNotPresent(BLUE_BED, POS, 3.0);
        helper.succeed();
    }

    private static void placeBed(GameTestHelper helper, Block bed) {
        final var state = bed.defaultBlockState().setValue(AbstractBedBlock.FACING, Direction.NORTH);
        helper.setBlock(HEAD, state.setValue(AbstractBedBlock.PART, BedPart.HEAD));
        helper.setBlock(POS, state.setValue(AbstractBedBlock.PART, BedPart.FOOT));
    }

    private static void destroy(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        player.gameMode.destroyBlock(helper.absolutePos(pos));
    }

    private static boolean isLocked(GameTestHelper helper, BlockPos pos) {
        return LockedBlocks.isLocked(helper.getLevel(), helper.absolutePos(pos));
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }
}
