package dev.seeruk.bundle.nicks;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.nicks.api.NicksEvents;
import dev.seeruk.bundle.nicks.mixin.CachedUserNameToIdResolverAccessor;
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
    private static final Map<UUID, Component> FORMATTED = new ConcurrentHashMap<>();

    private Nicknames() {}

    /**
     * Formats every stored nickname. Called when the server starts.
     */
    public static void load(MinecraftServer server) {
        FORMATTED.clear();
        STORAGE.get(server).forEach((id, nickname) -> FORMATTED.put(id, nickname.format()));
    }

    public static void clear() {
        FORMATTED.clear();
    }

    /**
     * The player's formatted nickname, without the prefix.
     */
    public static Optional<Component> get(UUID id) {
        return Optional.ofNullable(FORMATTED.get(id));
    }

    /**
     * The player's formatted nickname with the prefix, as it's shown in place of their name.
     */
    public static Optional<Component> displayName(UUID id) {
        return get(id).map(nickname -> Component.literal(Nickname.PREFIX).append(nickname));
    }

    public static NickEditResult set(MinecraftServer server, NameAndId player, String input, boolean trusted) {
        final var nickname = new Nickname(input, trusted);
        final var formatted = nickname.format();

        final var failure = Nickname.validate(formatted, trusted, name -> isOtherUsername(server, player, name));
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
        FORMATTED.put(player.id(), formatted);

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

    private static NickEditResult.Edited changed(MinecraftServer server, NameAndId player, NickEditResult.Edited result) {
        NicksTabList.refresh(server, player.id());
        NicksEvents.CHANGED.invoker().onNickChanged(server, player, result.original(), result.edited());

        return result;
    }

    /**
     * Returns true if the name is a username of another player who has played on this server
     * before or is currently online.
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
}
