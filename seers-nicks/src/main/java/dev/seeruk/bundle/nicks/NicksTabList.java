package dev.seeruk.bundle.nicks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Utilities for formatting and refreshing the tab list nicknames.
 */
public final class NicksTabList {
    private NicksTabList() {}

    /**
     * The player's nickname in the tab list with their real name in parentheses, or empty.
     */
    public static Optional<Component> name(ServerPlayer player) {
        return Nicknames.displayName(player.getUUID()).map(nickname -> Component.empty()
            .append(PlayerTeam.formatNameForTeam(player.getTeam(), nickname))
            .append(Component.literal(" (" + player.getGameProfile().name() + ")").withStyle(ChatFormatting.GRAY)));
    }

    /**
     * Sends everyone the player's current tab list name, if they're online.
     */
    public static void refresh(MinecraftServer server, UUID id) {
        final var player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            send(server, List.of(player));
        }
    }

    /**
     * Sends everyone the current tab list names of any named players that are online and have
     * a nickname (e.g. after their team changed).
     */
    public static void refreshByName(MinecraftServer server, Collection<String> names) {
        // Teams can change while the world is still loading, before there are any players
        final var playerList = server.getPlayerList();
        if (playerList == null) {
            return;
        }

        final var players = names.stream()
            .map(playerList::getPlayerByName)
            .filter(Objects::nonNull)
            .filter(player -> Nicknames.get(player.getUUID()).isPresent())
            .toList();

        if (!players.isEmpty()) {
            send(server, players);
        }
    }

    private static void send(MinecraftServer server, Collection<ServerPlayer> players) {
        final var actions = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(actions, players));
    }
}
