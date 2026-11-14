package dev.seeruk.bundle.core.mixin;

import dev.seeruk.bundle.core.tablist.TabList;
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
 * Refreshes players' tab list names when their team changes, as names changed by mods include the
 * team's styling (see {@link TabList}).
 */
@Mixin(ServerScoreboard.class)
abstract class ServerScoreboardMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "addPlayerToTeam", at = @At("RETURN"))
    private void core$refreshAdded(String player, PlayerTeam team, CallbackInfoReturnable<Boolean> cir) {
        TabList.refreshByName(server, List.of(player));
    }

    @Inject(method = "removePlayerFromTeam", at = @At("TAIL"))
    private void core$refreshRemoved(String player, PlayerTeam team, CallbackInfo ci) {
        TabList.refreshByName(server, List.of(player));
    }

    @Inject(method = "onTeamChanged", at = @At("TAIL"))
    private void core$refreshChanged(PlayerTeam team, CallbackInfo ci) {
        TabList.refreshByName(server, team.getPlayers());
    }

    @Inject(method = "onTeamRemoved", at = @At("TAIL"))
    private void core$refreshRemovedTeam(PlayerTeam team, CallbackInfo ci) {
        TabList.refreshByName(server, team.getPlayers());
    }
}
