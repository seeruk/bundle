package dev.seeruk.bundle.nicks;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.nicks.api.NicksEvents;
import dev.seeruk.bundle.nicks.mixin.CachedUserNameToIdResolverAccessor;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.nicks.SeersNicks.MOD_ID;

/**
 * Stores, looks up, and changes players' nicknames.
 */
public final class Nicknames {
    private static final WorldStorage<Map<UUID, Nickname>> STORAGE = WorldStorage.create(
        MOD_ID,
        "nicknames",
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Nickname.CODEC),
        Map::of
    );

    // Formatted nicknames, so looking one up (which happens for every chat message) doesn't need to
    // parse anything or touch world data. Concurrent, as names are looked up from many threads.
    private static final Map<UUID, Formatted> FORMATTED = new ConcurrentHashMap<>();

    private Nicknames() {}

    /**
     * Formats every stored nickname. Called when the server starts.
     */
    public static void load(MinecraftServer server) {
        FORMATTED.clear();
        STORAGE.get(server).forEach((id, nickname) -> FORMATTED.put(id, new Formatted(nickname.format())));
    }

    public static void clear() {
        FORMATTED.clear();
    }

    /**
     * The player's formatted nickname, without the prefix.
     */
    public static Optional<Component> get(UUID id) {
        return Optional.ofNullable(FORMATTED.get(id)).map(Formatted::nickname);
    }

    /**
     * The player's formatted nickname with the prefix, as it's shown in place of their name.
     */
    public static Optional<Component> displayName(UUID id) {
        return Optional.ofNullable(FORMATTED.get(id)).map(Formatted::displayName);
    }

    public static NickEditResult set(MinecraftServer server, NameAndId player, String input, boolean trusted) {
        final var nickname = new Nickname(input, trusted);
        final var formatted = nickname.format();

        final var failure = Nickname.validate(
            formatted,
            trusted,
            name -> isOtherUsername(server, player, name),
            name -> isOtherNickname(player, name)
        );
        if (failure.isPresent()) {
            return failure.get();
        }

        final var original = get(player.id());
        final var result = new NickEditResult.Edited(original, Optional.of(formatted));
        if (original.map(formatted::equals).orElse(false)) {
            // We specifically don't save here, as if the nickname hasn't changed, we don't want to
            // deny it, but we also don't want to do things like fire events.
            return result;
        }

        STORAGE.update(server, all -> {
            final var updated = new HashMap<>(all);
            updated.put(player.id(), nickname);
            return Map.copyOf(updated);
        });
        FORMATTED.put(player.id(), new Formatted(formatted));

        return changed(server, player, result);
    }

    public static NickEditResult reset(MinecraftServer server, NameAndId player) {
        final var original = get(player.id());
        if (original.isEmpty()) {
            return new NickEditResult.NoNickname();
        }

        STORAGE.update(server, all -> {
            final var updated = new HashMap<>(all);
            updated.remove(player.id());
            return Map.copyOf(updated);
        });
        FORMATTED.remove(player.id());

        return changed(server, player, new NickEditResult.Edited(original, Optional.empty()));
    }

    /**
     * Resets any nickname a player set that's the username of a player who's just joined, as it
     * was set before the server knew about them. Nicknames set by admins are left alone.
     */
    public static void releaseUsername(MinecraftServer server, NameAndId joined) {
        // Get the stored nicknames, filter to user-set nicknames that aren't the player's own, then
        // grab the map for that, and check if its plain text is equal to the joined players name.
        final var clashing = STORAGE.get(server).entrySet().stream()
            .filter(entry -> !entry.getKey().equals(joined.id()) && !entry.getValue().trusted())
            .map(Map.Entry::getKey)
            .filter(id -> get(id).map(Component::getString).filter(joined.name()::equalsIgnoreCase).isPresent())
            .toList();

        for (final var id : clashing) {
            reset(server, owner(server, id));

            final var owner = server.getPlayerList().getPlayer(id);
            if (owner != null) {
                owner.sendSystemMessage(TextParsers.parseAsTrusted(
                    "<yellow>Your nickname has been reset, as it's now another player's username</yellow>"
                ));
            }
        }
    }

    /**
     * The nickname's owner, named from the server's cache. The cache may have forgotten them if
     * they haven't played in a long time, so their UUID stands in for their name then.
     */
    private static NameAndId owner(MinecraftServer server, UUID id) {
        return server.services().nameToIdCache().get(id).orElseGet(() -> new NameAndId(id, id.toString()));
    }

    private static NickEditResult.Edited changed(MinecraftServer server, NameAndId player, NickEditResult.Edited result) {
        NicksTabList.refresh(server, player.id());
        NicksEvents.CHANGED.invoker().onNickChanged(server, player, result.original(), result.edited());

        return result;
    }

    /**
     * Returns true if the name is the username of another player who's online, or that the server
     * has cached (e.g. because they've played here before, or an admin has looked them up).
     */
    private static boolean isOtherUsername(MinecraftServer server, NameAndId player, String name) {
        if (name.equalsIgnoreCase(player.name())) {
            return false;
        }

        if (server.getPlayerList().getPlayerByName(name) != null) {
            return true;
        }

        return server.services().nameToIdCache() instanceof CachedUserNameToIdResolverAccessor cache
            && cache.getProfilesByName().containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns true if the name is another player's nickname, ignoring formatting and case.
     */
    private static boolean isOtherNickname(NameAndId player, String name) {
        return FORMATTED.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(player.id())
            && entry.getValue().nickname().getString().equalsIgnoreCase(name));
    }

    /**
     * A formatted nickname, and its prefixed display name, so lookups don't build anything.
     */
    private record Formatted(Component nickname, Component displayName) {
        private Formatted(Component nickname) {
            this(nickname, Component.literal(Nickname.PREFIX).append(nickname));
        }
    }
}
