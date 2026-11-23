package dev.seeruk.bundle.resourcezones.edit;

import dev.seeruk.bundle.resourcezones.ResourceZones;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service for players' edits to resource zones (see {@link ZoneEdit}). Edits are only kept in
 * memory, until they're committed or discarded, or the player leaves.
 *
 * <p>Edits are kept by UUID, so they survive respawning (which makes a new player).
 */
public final class ZoneEditor {
    // Player -> their edit. Concurrent, as it can be read from many threads.
    private static final Map<UUID, ZoneEdit> EDITS = new ConcurrentHashMap<>();

    private ZoneEditor() {}

    public static Optional<ZoneEdit> edit(ServerPlayer player) {
        return Optional.ofNullable(EDITS.get(player.getUUID()));
    }

    public static boolean isEditing(ServerPlayer player) {
        return EDITS.containsKey(player.getUUID());
    }

    /**
     * Starts an edit in the given mode, or switches the player's current edit to it.
     */
    public static ZoneEdit start(ServerPlayer player, EditMode mode) {
        return EDITS.merge(player.getUUID(), ZoneEdit.start(mode), (edit, started) -> edit.withMode(mode));
    }

    /**
     * Pauses the player's edit, if they have one, returning it.
     */
    public static Optional<ZoneEdit> pause(ServerPlayer player) {
        return Optional.ofNullable(
            EDITS.computeIfPresent(player.getUUID(), (uuid, edit) -> edit.withMode(EditMode.PAUSED))
        );
    }

    /**
     * Saves the player's edit to resource zones, one change per dimension, and ends it, returning
     * the edit if they had one.
     */
    public static Optional<ZoneEdit> commit(MinecraftServer server, ServerPlayer player) {
        final var edit = discard(player);
        edit.ifPresent(committed -> committed.pending().keySet().forEach(dimension -> ResourceZones.apply(
            server,
            dimension,
            committed.added(dimension),
            committed.removed(dimension)
        )));

        return edit;
    }

    /**
     * Ends the player's edit without saving it, returning the edit if they had one.
     */
    public static Optional<ZoneEdit> discard(ServerPlayer player) {
        return Optional.ofNullable(EDITS.remove(player.getUUID()));
    }

    public static void markAll(MinecraftServer server) {
        for (final var player : server.getPlayerList().getPlayers()) {
            mark(player);
        }
    }

    /**
     * Marks the chunk the player's in, if they're editing.
     */
    public static void mark(ServerPlayer player) {
        final var level = player.level();
        EDITS.computeIfPresent(
            player.getUUID(), (uuid, edit) -> edit.mark(
                level.dimension(),
                player.chunkPosition(),
                ResourceZones.chunks(level.getServer(), level.dimension())
            )
        );
    }

    /**
     * Throws the player's edit away, e.g. because they're disconnecting.
     */
    public static void forget(ServerPlayer player) {
        EDITS.remove(player.getUUID());
    }

    public static void clear() {
        EDITS.clear();
    }
}
