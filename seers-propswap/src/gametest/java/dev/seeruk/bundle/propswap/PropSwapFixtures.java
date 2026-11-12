package dev.seeruk.bundle.propswap;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared helpers for PropSwap's GameTests.
 */
public final class PropSwapFixtures {
    /**
     * Where tests put the block they're testing.
     */
    public static final BlockPos POS = new BlockPos(1, 1, 1);

    private PropSwapFixtures() {}

    /**
     * A survival player holding the given tool, with PropSwap enabled.
     */
    @SuppressWarnings("removal")
    public static ServerPlayer player(GameTestHelper helper, Item tool) {
        final var player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
        PropSwapMode.enable(player);
        return player;
    }

    /**
     * Uses the player's main hand on top of the block at {@link #POS}, via Fabric's event.
     */
    public static InteractionResult use(GameTestHelper helper, ServerPlayer player) {
        final var pos = helper.absolutePos(POS);
        final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
    }
}
