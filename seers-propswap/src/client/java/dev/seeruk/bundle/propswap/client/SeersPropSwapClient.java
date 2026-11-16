package dev.seeruk.bundle.propswap.client;

import dev.seeruk.bundle.propswap.PropSwapTargets;
import dev.seeruk.bundle.propswap.SeersPropSwap;
import dev.seeruk.bundle.propswap.network.PropSwapModePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The optional client part of PropSwap. While PropSwap is on, it stops the client predicting
 * Vanilla's own tool actions (stripping logs, scraping copper, etc.) on blocks PropSwap will
 * change, so they don't flicker. The click still goes to the server, which does the PropSwap.
 *
 * <p>It's only ever on when the server says so, nothing changes on servers without PropSwap.
 */
public class SeersPropSwapClient implements ClientModInitializer {
    private static volatile boolean enabled = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            PropSwapModePayload.TYPE, (payload, context) ->
                enabled = payload.enabled()
        );

        // It's always off on a new connection, until the server says otherwise
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> enabled = false);
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> enabled = false);

        UseBlockCallback.EVENT.register(SeersPropSwap.LATE_PHASE, SeersPropSwapClient::interact);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Returning {@code SUCCESS} on the client sends the click to the server without the client
     * handling it itself, stopping unintended client-side actions like flickering / sounds.
     */
    private static InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() || !enabled || player.isSpectator()) {
            return InteractionResult.PASS;
        }

        final var state = level.getBlockState(hit.getBlockPos());
        return PropSwapTargets.allowed(player, player.getItemInHand(hand), state, hit)
            ? InteractionResult.SUCCESS
            : InteractionResult.PASS;
    }
}
