package dev.seeruk.bundle.core.mixin;

import dev.seeruk.bundle.core.action.CustomActions;
import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Since Vanilla doesn't pass player info along with Dialog custom actions
 * ({@code minecraft:custom}), this mixin enables us to pass it along to {@link CustomActions}.
 *
 * <p>It also passes every packet sent to a player to {@link ActionBarMessages}, so they know when
 * something else uses the action bar.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerCommonPacketListenerImplMixin {
    @Inject(
        method = "handleCustomClickAction",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"
        )
    )
    private void core$handleCustomAction(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        // Only players in the game itself, e.g. not ones still configuring their connection
        if ((Object) this instanceof ServerGamePacketListenerImpl game) {
            CustomActions.handle(game.player, packet.id(), packet.payload().orElse(null));
        }
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
        at = @At("HEAD")
    )
    private void core$notePacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if ((Object) this instanceof ServerGamePacketListenerImpl game) {
            ActionBarMessages.noteOtherMessage(game.player, packet);
        }
    }
}
