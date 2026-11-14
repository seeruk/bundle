package dev.seeruk.bundle.afk.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.seeruk.bundle.afk.command.AfkCommand;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Changes what counts as a player doing something (see {@link ServerPlayer#getLastActionTime()}).
 * Players who are only being moved by something else aren't doing anything, and players looking
 * around are.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @WrapWithCondition(
        method = "handlePlayerKnownMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V")
    )
    private boolean afk$ignoreMovement(ServerPlayer player) {
        // Players moving themselves press keys, which counts anyway
        return false;
    }

    @Inject(
        method = "handleMovePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER
        )
    )
    private void afk$countLookingAround(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (packet.hasRotation() && (packet.getYRot(0) != player.getYRot() || packet.getXRot(0) != player.getXRot())) {
            player.resetLastActionTime();
        }
    }

    @WrapWithCondition(
        method = "tryHandleChat",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V")
    )
    private boolean afk$ignoreAfkCommand(ServerPlayer player, String message, boolean isCommand) {
        // /afk decides the status itself. Counting it would bring players back before it runs.
        return !isCommand || !AfkCommand.isAfkCommand(message);
    }
}
