package dev.seeruk.bundle.nicks.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.seeruk.bundle.nicks.NicksTabList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives nicknamed players their nickname in the tab list.
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
    private Component nicks$useNickname(Component original) {
        return NicksTabList.name((ServerPlayer) (Object) this).orElse(original);
    }
}
