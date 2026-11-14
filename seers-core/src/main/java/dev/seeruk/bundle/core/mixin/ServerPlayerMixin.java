package dev.seeruk.bundle.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.seeruk.bundle.core.tablist.TabList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives players the tab list name and position that mods have asked for (see {@link TabList}).
 */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
    private Component core$tabListName(Component original) {
        return TabList.name((ServerPlayer) (Object) this, original);
    }

    @ModifyReturnValue(method = "getTabListOrder", at = @At("RETURN"))
    private int core$tabListOrder(int original) {
        return TabList.ORDER.invoker().order((ServerPlayer) (Object) this, original);
    }
}
