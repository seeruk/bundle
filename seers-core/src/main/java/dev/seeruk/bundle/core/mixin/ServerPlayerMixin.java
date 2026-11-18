package dev.seeruk.bundle.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.seeruk.bundle.core.api.PlayerEvents;
import dev.seeruk.bundle.core.tablist.TabList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives players the tab list name and position that mods have asked for (see {@link TabList}),
 * and fires {@link PlayerEvents#DEATH_MESSAGE} when a player's death message is shown.
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

    // The death message is only made when it's going to be shown (i.e. with the showDeathMessages
    // game rule on). It has to be read here, as the combat tracker it comes from is reset by the
    // end of dying, so it'd only say "<player> died" after.
    @ModifyExpressionValue(
        method = "die",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"
        )
    )
    private Component core$deathMessage(Component message) {
        PlayerEvents.DEATH_MESSAGE.invoker().onDeathMessage((ServerPlayer) (Object) this, message);
        return message;
    }
}
