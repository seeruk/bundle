package dev.seeruk.bundle.propswap;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.propswap.api.PropSwapEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.*;

public class PropSwapInteractionGameTest {
    private static final EventLog<Swapped> EVENTS = new EventLog<>();

    static {
        PropSwapEvents.SWAPPED.register((player, level, pos, original, swapped) ->
            EVENTS.record(new Swapped(player, pos, original, swapped)));
    }

    @GameTest
    public void swapsBlockAndFiresEvent(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        helper.setBlock(POS, Blocks.FURNACE);
        final var original = helper.getBlockState(POS);

        final var result = use(helper, player);

        final var swapped = original.setValue(AbstractFurnaceBlock.LIT, true);
        helper.assertValueEqual(result, InteractionResult.SUCCESS, "interaction result");
        helper.assertValueEqual(helper.getBlockState(POS), swapped, "block state");
        helper.assertValueEqual(
            eventsFor(player),
            List.of(new Swapped(player, helper.absolutePos(POS), original, swapped)),
            "events"
        );
        helper.succeed();
    }

    @GameTest
    public void refusedSwapDoesNotFireEvent(GameTestHelper helper) {
        // Crouching on a side toggles the post, but a wall with only a post can't lose it
        final var player = player(helper, Items.IRON_PICKAXE);
        player.setShiftKeyDown(true);
        helper.setBlock(POS, Blocks.COBBLESTONE_WALL);
        final var original = helper.getBlockState(POS);

        final var result = use(helper, player, Direction.NORTH);

        helper.assertValueEqual(result, InteractionResult.SUCCESS, "interaction result");
        helper.assertValueEqual(helper.getBlockState(POS), original, "block state");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired for a refused swap");
        helper.succeed();
    }

    @GameTest
    public void doesNothingWhenDisabled(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        PropSwapMode.disable(player);

        assertNotSwapped(helper, player, Blocks.FURNACE);
        helper.succeed();
    }

    @GameTest
    public void doesNothingWithWrongTool(GameTestHelper helper) {
        assertNotSwapped(helper, player(helper, Items.IRON_AXE), Blocks.FURNACE);
        helper.succeed();
    }

    @GameTest
    public void doesNothingForUnsupportedBlock(GameTestHelper helper) {
        assertNotSwapped(helper, player(helper, Items.IRON_PICKAXE), Blocks.STONE);
        helper.succeed();
    }

    @GameTest
    public void doesNothingInAdventureMode(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        player.setGameMode(GameType.ADVENTURE);

        assertNotSwapped(helper, player, Blocks.FURNACE);
        helper.succeed();
    }

    @GameTest
    public void doesNothingForSpectators(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_PICKAXE);
        player.setGameMode(GameType.SPECTATOR);

        assertNotSwapped(helper, player, Blocks.FURNACE);
        helper.succeed();
    }

    private record Swapped(Player player, BlockPos pos, BlockState original, BlockState swapped) {}

    private static List<Swapped> eventsFor(Player player) {
        return EVENTS.matching(event -> event.player() == player);
    }

    private static void assertNotSwapped(GameTestHelper helper, ServerPlayer player, Block block) {
        helper.setBlock(POS, block);
        final var original = helper.getBlockState(POS);

        final var result = use(helper, player);

        helper.assertValueEqual(result, InteractionResult.PASS, "interaction result");
        helper.assertValueEqual(helper.getBlockState(POS), original, "block state");
        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired without a swap");
    }
}
