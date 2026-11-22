package dev.seeruk.bundle.resourcezones.export;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.ResourceZonesConfig;
import dev.seeruk.bundle.resourcezones.SeersResourceZones;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;

// Each test exports its own made-up dimension, so no other test's chunks end up in its file
public class ZoneExportsGameTest {
    private static final String FOLDER = "zone-exports-gametest";

    @GameTest
    public void writesTheDimensionsChunks(GameTestHelper helper) throws IOException {
        final var server = helper.getLevel().getServer();
        final var dimension = dimension("written");
        ResourceZones.add(server, dimension, new ChunkPos(1, 2));
        ResourceZones.add(server, dimension, new ChunkPos(-40, 3));

        ZoneExports.export(server, FOLDER, dimension);

        helper.assertValueEqual(read(helper, FOLDER, dimension), "-2;0;-40;3\n0;0;1;2\n", "file");
        helper.succeed();
    }

    @GameTest
    public void writesAnEmptyFileWithoutChunks(GameTestHelper helper) throws IOException {
        final var dimension = dimension("empty");

        ZoneExports.export(helper.getLevel().getServer(), FOLDER, dimension);

        helper.assertValueEqual(read(helper, FOLDER, dimension), "", "file");
        helper.succeed();
    }

    @GameTest
    public void writesTheConfiguredFolderWhenZonesChange(GameTestHelper helper) throws IOException {
        final var folder = ModConfig.load(SeersResourceZones.MOD_ID, ResourceZonesConfig.CODEC).export().folder();
        final var dimension = dimension("changed");

        ResourceZones.add(helper.getLevel().getServer(), dimension, new ChunkPos(5, 6));

        helper.assertValueEqual(read(helper, folder, dimension), "0;0;5;6\n", "file");
        helper.succeed();
    }

    @GameTest
    public void writesNothingOutsideTheServersFolder(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var dimension = dimension("outside");

        ZoneExports.export(server, "../" + FOLDER, dimension);

        helper.assertFalse(
            Files.exists(server.getServerDirectory().resolve(ZoneCsv.path("../" + FOLDER, dimension))),
            "File was written outside the server's folder"
        );
        helper.succeed();
    }

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("seers-resource-zones-gametest", name)
        );
    }

    private static String read(GameTestHelper helper, String folder, ResourceKey<Level> dimension) throws IOException {
        final var directory = helper.getLevel().getServer().getServerDirectory();
        return Files.readString(directory.resolve(ZoneCsv.path(folder, dimension)));
    }
}
