package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.afk.api.AfkEvents;
import dev.seeruk.bundle.afk.api.AfkPlayers;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends players going AFK, and coming back, to Discord, and checks whether players are AFK. Seer's
 * AFK is optional, so this is its own class, which is only loaded (and so only needs the AFK mod)
 * if the AFK mod is.
 */
final class AfkEventsBridge {
    private AfkEventsBridge() {}

    static void register(EventsConfig events) {
        AfkEvents.STATUS_CHANGED.register((player, isAfk, reason) ->
            DiscordRelay.afk(player, isAfk, events).ifPresent(DiscordBot::send));
    }

    static boolean isAfk(ServerPlayer player) {
        return AfkPlayers.isAfk(player);
    }
}
