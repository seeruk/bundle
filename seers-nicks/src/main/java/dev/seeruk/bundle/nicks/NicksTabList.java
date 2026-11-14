package dev.seeruk.bundle.nicks;

import dev.seeruk.bundle.core.tablist.TabList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.UUID;

/**
 * Shows nicknames in the tab list, through core's {@link TabList}.
 */
public final class NicksTabList {
    private NicksTabList() {}

    /**
     * Returns a nicknamed player's tab list name with their nickname, followed by their real name
     * in parentheses.
     */
    public static Component name(ServerPlayer player, Component name) {
        return Nicknames.displayName(player.getUUID())
            .<Component>map(nickname -> Component.empty()
                .append(PlayerTeam.formatNameForTeam(player.getTeam(), nickname))
                .append(Component.literal(" (" + player.getGameProfile().name() + ")").withStyle(ChatFormatting.GRAY)))
            .orElse(name);
    }

    /**
     * Sends everyone the player's current tab list name, if they're online.
     */
    public static void refresh(MinecraftServer server, UUID id) {
        final var player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            TabList.refresh(player);
        }
    }
}
