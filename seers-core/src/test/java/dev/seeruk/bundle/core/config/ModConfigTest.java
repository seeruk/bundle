package dev.seeruk.bundle.core.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModConfigTest {
    private record TestConfig(String name, int count, Section section) {
        static final Codec<TestConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(TestConfig::name),
            Codec.INT.fieldOf("count").forGetter(TestConfig::count),
            Section.CODEC.fieldOf("section").forGetter(TestConfig::section)
        ).apply(instance, TestConfig::new));
    }

    private record Section(boolean enabled, double ratio) {
        static final Codec<Section> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(Section::enabled),
            Codec.DOUBLE.fieldOf("ratio").forGetter(Section::ratio)
        ).apply(instance, Section::new));
    }

    private static final String DIST = """
        # The name of the thing.
        name: default
        
        count: 1
        
        section:
          enabled: false
          ratio: 0.5
        """;

    @TempDir
    private Path folder;

    @Test
    void usesTheDefaultsWithoutAUserFile() {
        final var loaded = ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC);

        assertEquals(new TestConfig("default", 1, new Section(false, 0.5)), loaded.config());
        assertTrue(loaded.usedDefaults(), "Didn't say it used the defaults");
    }

    @Test
    void mergesTheUserFileOverTheDefaults() throws IOException {
        Files.writeString(folder.resolve(ModConfig.USER_FILE), "count: 3\nsection:\n  enabled: true\n");

        final var loaded = ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC);

        assertEquals(new TestConfig("default", 3, new Section(true, 0.5)), loaded.config());
        assertFalse(loaded.usedDefaults(), "Said it used the defaults");
    }

    @Test
    void emptyUserFileUsesTheDefaults() throws IOException {
        Files.writeString(folder.resolve(ModConfig.USER_FILE), "# Nothing changed yet\n");

        assertEquals(
            new TestConfig("default", 1, new Section(false, 0.5)),
            ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC).config()
        );
    }

    @Test
    void reportsUnknownSettings() throws IOException {
        Files.writeString(folder.resolve(ModConfig.USER_FILE), "cuont: 3\nsection:\n  enabld: true\n");

        assertEquals(List.of("cuont", "section.enabld"), ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC).unknownKeys());
    }

    @Test
    void refusesInvalidSettings() throws IOException {
        Files.writeString(folder.resolve(ModConfig.USER_FILE), "count: lots\n");

        final var e = assertThrows(ConfigException.class, () -> ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC));
        assertTrue(e.getMessage().contains(ModConfig.USER_FILE), "Error doesn't say which file: " + e.getMessage());
    }

    @Test
    void emptyDistSettingsCanBeOptional() throws IOException {
        final var codec = Codec.STRING.optionalFieldOf("token").codec();
        final var dist = "# Left empty, so it's off until it's set.\ntoken:\n";
        assertEquals(Optional.empty(), ModConfig.load(folder, ModConfig.CONFIG, dist, codec).config());

        Files.writeString(folder.resolve(ModConfig.USER_FILE), "token: secret\n");
        assertEquals(Optional.of("secret"), ModConfig.load(folder, ModConfig.CONFIG, dist, codec).config());
    }

    @Test
    void loadsOtherConfigFilesByName() throws IOException {
        Files.writeString(folder.resolve("book.yml"), "count: 3\n");

        final var loaded = ModConfig.load(folder, "book", DIST, TestConfig.CODEC);

        assertEquals(3, loaded.config().count());
        assertEquals(DIST, Files.readString(folder.resolve("book.dist.yml")));
        assertTrue(!Files.exists(folder.resolve(ModConfig.DIST_FILE)), "Wrote the main config's dist file");
    }

    @Test
    void writesTheDistFile() throws IOException {
        ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC);

        assertEquals(DIST, Files.readString(folder.resolve(ModConfig.DIST_FILE)));
    }

    @Test
    void replacesAnOutdatedDistFile() throws IOException {
        Files.writeString(folder.resolve(ModConfig.DIST_FILE), "name: outdated\n");

        ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC);

        assertEquals(DIST, Files.readString(folder.resolve(ModConfig.DIST_FILE)));
    }

    @Test
    void leavesAnUpToDateDistFileAlone() throws IOException {
        final var dist = folder.resolve(ModConfig.DIST_FILE);
        Files.writeString(dist, DIST);
        final var modified = FileTime.fromMillis(0);
        Files.setLastModifiedTime(dist, modified);

        ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC);

        assertEquals(modified, Files.getLastModifiedTime(dist));
    }

    @Test
    void stillLoadsWhenTheDistFileCantBeWritten() throws IOException {
        // A folder where the dist file should be, so it can't be written
        Files.createDirectories(folder.resolve(ModConfig.DIST_FILE));

        final var loaded = ModConfig.load(folder, ModConfig.CONFIG, DIST, TestConfig.CODEC);

        assertEquals("default", loaded.config().name());
        assertTrue(loaded.distWriteFailure().isPresent(), "Didn't report the failure");
    }
}
