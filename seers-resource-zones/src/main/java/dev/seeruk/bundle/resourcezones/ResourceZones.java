package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.function.UnaryOperator;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.MOD_ID;

/**
 * A service for interacting with resource zones and their chunks, in world storage.
 */
public final class ResourceZones {
    private static final WorldStorage<ZoneChunks> STORAGE = WorldStorage.create(
        MOD_ID,
        "zone_chunks",
        ZoneChunks.CODEC,
        () -> ZoneChunks.EMPTY
    );

    private ResourceZones() {}

    public static boolean contains(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos pos) {
        return STORAGE.get(server).contains(dimension, pos);
    }

    /**
     * The chunks in zones in the dimension.
     */
    public static Set<ChunkPos> chunks(MinecraftServer server, ResourceKey<Level> dimension) {
        return STORAGE.get(server).in(dimension);
    }

    /**
     * Adds the chunk to the resource zones, returning whether it wasn't in one already.
     */
    public static boolean add(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos pos) {
        return change(server, dimension, zones -> zones.with(dimension, pos));
    }

    /**
     * Removes the chunk from the resource zones, returning whether it was in one.
     */
    public static boolean remove(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos pos) {
        return change(server, dimension, zones -> zones.without(dimension, pos));
    }

    /**
     * Saves the change to the dimension's zone chunks, and fires {@link ResourceZoneEvents#CHANGED}
     * if anything changed, returning whether it did.
     */
    private static boolean change(
        MinecraftServer server,
        ResourceKey<Level> dimension,
        UnaryOperator<ZoneChunks> change
    ) {
        final var current = STORAGE.get(server);
        final var changed = change.apply(current);
        if (changed.equals(current)) {
            return false;
        }

        STORAGE.set(server, changed);
        ResourceZoneEvents.CHANGED.invoker().onChanged(server, dimension);
        return true;
    }
}
