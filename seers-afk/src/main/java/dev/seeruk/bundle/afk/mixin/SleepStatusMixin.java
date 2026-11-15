package dev.seeruk.bundle.afk.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.seeruk.bundle.afk.AfkStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Leaves AFK players out of the sleep count, as Vanilla does for spectators, so they can't stop
 * everyone else from skipping the night.
 */
@Mixin(SleepStatus.class)
abstract class SleepStatusMixin {
    @WrapOperation(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z")
    )
    private boolean afk$skipAfkPlayers(ServerPlayer player, Operation<Boolean> original) {
        return original.call(player) || AfkStatus.isAfk(player);
    }
}
