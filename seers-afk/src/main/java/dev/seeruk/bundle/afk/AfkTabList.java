package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Marks AFK players in the tab list, and moves them to the bottom.
 */
public final class AfkTabList {
    private AfkTabList() {}

    /**
     * The player's tab list name: {@code template} with {@code name} filled in if they're AFK, or
     * {@code name} as it is if they're not.
     *
     * @param player   The player being shown.
     * @param name     How the player would be shown otherwise.
     * @param template How AFK players are shown, using {@code ${tab_list_name}} for {@code name}.
     */
    public static Component name(ServerPlayer player, Component name, TextTemplate template) {
        return AfkStatus.isAfk(player) ? template.format(Map.of("tab_list_name", name)) : name;
    }

    /**
     * The player's tab list order, moved down by one if they're AFK.
     */
    public static int order(ServerPlayer player, int order) {
        return AfkStatus.isAfk(player) ? order - 1 : order;
    }
}
