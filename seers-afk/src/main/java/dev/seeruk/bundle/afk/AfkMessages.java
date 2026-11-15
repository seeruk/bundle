package dev.seeruk.bundle.afk;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Tells everyone when a player goes AFK, or comes back.
 */
public final class AfkMessages {
    private AfkMessages() {}

    public static void announce(ServerPlayer player, boolean isAfk, AfkConfig.Messages messages) {
        player.level().getServer().getPlayerList().broadcastSystemMessage(message(player, isAfk, messages), false);
    }

    static Component message(ServerPlayer player, boolean isAfk, AfkConfig.Messages messages) {
        final var template = isAfk ? messages.afk() : messages.back();
        return template.format(Map.of("display_name", player.getDisplayName()));
    }
}
