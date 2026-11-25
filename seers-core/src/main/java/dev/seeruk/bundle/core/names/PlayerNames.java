package dev.seeruk.bundle.core.names;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Optional;
import java.util.UUID;

/**
 * Players' names, whether they're online or not. Mods can change the name players are shown by
 * (e.g. to a nickname) with {@link #NAME}, without other mods needing to know about them.
 */
public final class PlayerNames {
    /**
     * Gives the name a player is shown by in place of their username, without team styling, which
     * is added after. The first listener to give a name wins.
     */
    public static final Event<Name> NAME = EventFactory.createArrayBacked(
        Name.class,
        (listeners) -> (server, id) -> {
            for (Name listener : listeners) {
                final var name = listener.name(server, id);
                if (name.isPresent()) {
                    return name;
                }
            }
            return Optional.empty();
        }
    );

    private PlayerNames() {}

    @FunctionalInterface
    public interface Name {
        /**
         * @param server The server the player is on, or was last on.
         * @param id     The player's UUID.
         */
        Optional<Component> name(MinecraftServer server, UUID id);
    }

    /**
     * The name the player is shown by (e.g. their nickname), or their username, styled for their
     * team as Vanilla would. Unlike Vanilla's display name, it doesn't do anything when hovered or
     * clicked, so it suits being shown anywhere. Players the server has never seen are shown by
     * their UUID.
     */
    public static Component displayName(MinecraftServer server, UUID id) {
        final var username = username(server, id);
        final var name = NAME.invoker().name(server, id)
            .or(() -> username.map(Component::literal))
            .orElseGet(() -> Component.literal(id.toString()));

        final var team = username.map(server.getScoreboard()::getPlayersTeam).orElse(null);
        return PlayerTeam.formatNameForTeam(team, name);
    }

    /**
     * The player's username if they're online, or the last one the server saw if not.
     * Only the server's own records are checked.
     */
    public static Optional<String> username(MinecraftServer server, UUID id) {
        return Optional.ofNullable(server.getPlayerList().getPlayer(id))
            .map(player -> player.getGameProfile().name())
            .or(() -> server.services().nameToIdCache().get(id).map(NameAndId::name));
    }
}
