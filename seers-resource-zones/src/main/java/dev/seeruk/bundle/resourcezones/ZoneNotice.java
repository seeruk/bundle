package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import dev.seeruk.bundle.resourcezones.edit.ZoneEditor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * A notice kept in the action bar (see {@link ActionBarMessages}) while a player is in a resource
 * zone, so they know the area is reset regularly. It's hidden while they're editing zones, as
 * their editing status takes its place.
 */
public final class ZoneNotice {
    private ZoneNotice() {}

    /**
     * @param player The player who might see the notice.
     * @param notice The notice, as it's configured.
     */
    public static Optional<Component> message(ServerPlayer player, Component notice) {
        if (ZoneEditor.isEditing(player)) {
            return Optional.empty();
        }

        final var level = player.level();
        return ResourceZones.contains(level.getServer(), level.dimension(), player.chunkPosition())
            ? Optional.of(notice)
            : Optional.empty();
    }
}
