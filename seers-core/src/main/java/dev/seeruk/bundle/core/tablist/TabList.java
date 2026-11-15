package dev.seeruk.bundle.core.tablist;

import dev.seeruk.bundle.core.SeersCore;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Lets mods change how players are shown in the tab list, without getting in each other's way.
 * Each mod adds a listener that's given the player's name (or position) so far, and returns it
 * changed or as it is. Players nothing changes are left entirely to Vanilla.
 */
public final class TabList {
    /**
     * An early phase for listeners that replace the name entirely (e.g. nicknames), so they run
     * before listeners that add to it.
     */
    public static final Identifier REPLACE = SeersCore.id("replace");

    /**
     * Changes a player's tab list name. Listeners start from the name Vanilla would show, and
     * listeners in {@link #REPLACE} run first.
     */
    public static final Event<Name> NAME = EventFactory.createWithPhases(Name.class,
        (listeners) -> (player, name) -> {
            for (Name listener : listeners) {
                name = listener.name(player, name);
            }
            return name;
        },
        REPLACE, Event.DEFAULT_PHASE
    );

    /**
     * Changes a player's position in the tab list. Players with a higher order are shown first, and
     * Vanilla's order is 0.
     *
     * <p>Listeners should adjust the order they're given (e.g. {@code order - 1}) rather than
     * replace it, so changes from different listeners add up the same in any order.
     */
    public static final Event<Order> ORDER = EventFactory.createArrayBacked(Order.class,
        (listeners) -> (player, order) -> {
            for (Order listener : listeners) {
                order = listener.order(player, order);
            }
            return order;
        }
    );

    private TabList() {}

    @FunctionalInterface
    public interface Name {
        /**
         * @param player The player being shown.
         * @param name   Their name so far. Return it as it is to leave it unchanged.
         */
        Component name(ServerPlayer player, Component name);
    }

    @FunctionalInterface
    public interface Order {
        /**
         * @param player The player being shown.
         * @param order  Their order so far.
         */
        int order(ServerPlayer player, int order);
    }

    /**
     * The player's tab list name after every listener, or {@code original} if none changed it.
     */
    public static Component name(ServerPlayer player, Component original) {
        // Vanilla leaves the name to the client, which styles it for the player's team like this
        final var base = original != null
            ? original
            : PlayerTeam.formatNameForTeam(player.getTeam(), Component.literal(player.getGameProfile().name()));

        final var name = NAME.invoker().name(player, base);
        return name == base ? original : name;
    }

    /**
     * Sends everyone the player's current tab list name and position.
     */
    public static void refresh(ServerPlayer player) {
        refresh(player.level().getServer(), List.of(player));
    }

    /**
     * Sends everyone the current tab list names and positions of any players that are
     * online (e.g. after their team changed).
     */
    public static void refreshByName(MinecraftServer server, Collection<String> names) {
        // Teams can change while the world is still loading, before there are any players
        final var playerList = server.getPlayerList();
        if (playerList == null) {
            return;
        }

        final var players = names.stream().map(playerList::getPlayerByName).filter(Objects::nonNull).toList();
        if (!players.isEmpty()) {
            refresh(server, players);
        }
    }

    private static void refresh(MinecraftServer server, Collection<ServerPlayer> players) {
        final var actions = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
        );
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(actions, players));
    }
}
