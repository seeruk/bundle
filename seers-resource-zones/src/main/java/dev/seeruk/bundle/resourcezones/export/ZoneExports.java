package dev.seeruk.bundle.resourcezones.export;

import dev.seeruk.bundle.core.storage.ServerFiles;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.io.IOException;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.LOGGER;

/**
 * Writes each dimension's zone chunks to a file in the server's folder, as an MCA Selector
 * selection (see {@link ZoneCsv}).
 */
public final class ZoneExports {
    private ZoneExports() {}

    /**
     * Writes every dimension's file, so each has one, even if it has no zones.
     */
    public static void exportAll(MinecraftServer server, String folder) {
        for (final var dimension : server.levelKeys()) {
            export(server, folder, dimension);
        }
    }

    /**
     * For the given folder and dimension, writes the dimension's zone file.
     */
    public static void export(MinecraftServer server, String folder, ResourceKey<Level> dimension) {
        final var path = ZoneCsv.path(folder, dimension);
        try {
            ServerFiles.write(server, path, ZoneCsv.contents(ResourceZones.chunks(server, dimension)));
        } catch (IOException e) {
            LOGGER.warn("Couldn't export the resource zones in {} to {}", dimension.identifier(), path, e);
        }
    }
}
