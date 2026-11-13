package dev.seeruk.bundle.propswap.network;

import dev.seeruk.bundle.propswap.SeersPropSwap;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * Network payload that contains the current mode of PropSwap for a player.
 */
public record PropSwapModePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<PropSwapModePayload> TYPE = new Type<>(SeersPropSwap.id("mode"));

    public static final StreamCodec<ByteBuf, PropSwapModePayload> CODEC =
        ByteBufCodecs.BOOL.map(PropSwapModePayload::new, PropSwapModePayload::enabled);

    @Override
    public @NonNull Type<PropSwapModePayload> type() {
        return TYPE;
    }
}
