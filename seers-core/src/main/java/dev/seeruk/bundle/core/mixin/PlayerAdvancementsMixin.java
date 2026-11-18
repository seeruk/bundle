package dev.seeruk.bundle.core.mixin;

import dev.seeruk.bundle.core.api.PlayerEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link PlayerEvents#ADVANCEMENT_MESSAGE} when a player completes an advancement that's
 * announced in chat.
 */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(
        method = "award",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/AdvancementHolder;)V"
        )
    )
    private void core$completed(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
        // Vanilla announces it in a lambda, which is fragile to target, so this makes the same
        // checks it does, and the same announcement
        holder.value().display()
            .filter(DisplayInfo::announceToChat)
            .filter(display -> player.level().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES))
            .ifPresent(display -> PlayerEvents.ADVANCEMENT_MESSAGE.invoker().onAdvancementMessage(
                player,
                holder,
                display.type().createAnnouncement(holder, player)
            ));
    }
}
