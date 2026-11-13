package dev.seeruk.bundle.propswap.network;

import dev.seeruk.bundle.propswap.PropSwapMode;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Network handling for PropSwap. Payloads are only sent to clients that support them, therefore
 * Vanilla clients are unaffected and can continue to connect and play on servers with PropSwap.
 */
public final class PropSwapNetworking {
    private PropSwapNetworking() {}

    public static void init() {
        PayloadTypeRegistry.clientboundPlay()
            .register(PropSwapModePayload.TYPE, PropSwapModePayload.CODEC);
    }

    /**
     * Sends the player's current mode to their client, if it has the client part installed.
     */
    public static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (ServerPlayNetworking.canSend(serverPlayer, PropSwapModePayload.TYPE)) {
                ServerPlayNetworking.send(serverPlayer, new PropSwapModePayload(PropSwapMode.isEnabled(player)));
            }
        }
    }
}
