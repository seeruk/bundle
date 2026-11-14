package dev.seeruk.bundle.nicks.mixin;

import dev.seeruk.bundle.nicks.NicksTabList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Refreshes nicknamed players' tab list names when their team changes, as those names include the
 * team's styling.
 */
@Mixin(ServerScoreboard.class)
abstract class ServerScoreboardMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "addPlayerToTeam", at = @At("RETURN"))
    private void nicks$refreshAdded(String player, PlayerTeam team, CallbackInfoReturnable<Boolean> cir) {
        NicksTabList.refreshByName(server, List.of(player));
    }

    @Inject(method = "removePlayerFromTeam", at = @At("TAIL"))
    private void nicks$refreshRemoved(String player, PlayerTeam team, CallbackInfo ci) {
        NicksTabList.refreshByName(server, List.of(player));
    }

    @Inject(method = "onTeamChanged", at = @At("TAIL"))
    private void nicks$refreshChanged(PlayerTeam team, CallbackInfo ci) {
        NicksTabList.refreshByName(server, team.getPlayers());
    }

    @Inject(method = "onTeamRemoved", at = @At("TAIL"))
    private void nicks$refreshRemovedTeam(PlayerTeam team, CallbackInfo ci) {
        NicksTabList.refreshByName(server, team.getPlayers());
    }
}
