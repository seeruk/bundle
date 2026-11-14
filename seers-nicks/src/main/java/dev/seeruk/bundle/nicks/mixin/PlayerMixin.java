package dev.seeruk.bundle.nicks.mixin;

import dev.seeruk.bundle.nicks.Nicknames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Shows a player's nickname in place of their name, wherever the server shows their display name
 * (e.g. chat, death messages, advancements). Only the name is swapped, so team styling, and
 * hovering or clicking on the name, still work as they do in Vanilla.
 */
@Mixin(Player.class)
abstract class PlayerMixin {
    @ModifyArg(
        method = "getDisplayName",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
        ),
        index = 1
    )
    private Component nicks$useNickname(Component name) {
        // Only on the server, as in singleplayer the client shares the same nicknames
        if (!((Object) this instanceof ServerPlayer player)) {
            return name;
        }

        return Nicknames.displayName(player.getUUID()).orElse(name);
    }
}
