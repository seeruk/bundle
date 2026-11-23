package dev.seeruk.bundle.resourcezones.edit;

import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;

/**
 * A status kept in the action bar (see {@link ActionBarMessages}) while a player is editing
 * resource zones, with their mode and how many chunks they've changed.
 */
public final class ZoneEditStatus {
    private ZoneEditStatus() {}

    /**
     * @param player The player who might see the status.
     * @param status The status, as it's configured, with {@code ${mode}}, {@code ${added}} and
     *               {@code ${removed}} placeholders.
     */
    public static Optional<Component> message(ServerPlayer player, TextTemplate status) {
        return ZoneEditor.edit(player).map(edit -> status.format(Map.of(
            "mode", Component.literal(edit.mode().getSerializedName()),
            "added", Component.literal(String.valueOf(edit.added())),
            "removed", Component.literal(String.valueOf(edit.removed()))
        )));
    }
}
