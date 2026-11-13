package dev.seeruk.bundle.propswap.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("UnstableApiUsage")
public class SeersPropSwapClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(@NonNull ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();

            final var pos = singleplayer.getServer().computeOnServer(server -> {
                final var player = server.getPlayerList().getPlayers().getFirst();
                final var logPos = player.blockPosition().east(2);

                player.level().setBlockAndUpdate(logPos, Blocks.OAK_LOG.defaultBlockState());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "propswap");

                return logPos;
            });

            clientReceivesMode(context, pos);
            logIsSwappedWithoutBeingStripped(context, pos);
            clientReceivesModeTurningOff(context, singleplayer);
        }
    }

    private void clientReceivesMode(ClientGameTestContext context, BlockPos pos) {
        context.waitFor(client -> SeersPropSwapClient.isEnabled()
            && client.player != null
            && client.player.getMainHandItem().is(Items.IRON_AXE)
            && client.level != null
            && client.level.getBlockState(pos).is(Blocks.OAK_LOG));
    }

    private void logIsSwappedWithoutBeingStripped(ClientGameTestContext context, BlockPos pos) {
        // Uses the log the same way right-clicking it would, then checks what the client predicted
        final var predicted = context.computeOnClient(client -> {
            final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
            return client.level.getBlockState(pos);
        });

        if (!predicted.is(Blocks.OAK_LOG)) {
            throw new AssertionError("Expected the client not to strip the log, but got " + predicted);
        }

        context.waitFor(client -> client.level.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z);
    }

    private void clientReceivesModeTurningOff(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            final var player = server.getPlayerList().getPlayers().getFirst();
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "propswap");
        });

        context.waitFor(client -> !SeersPropSwapClient.isEnabled());
    }
}
