package dev.seeruk.bundle.core.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utilities for handling a mod's config.
 *
 * <p>Each mod ships its documented defaults as a resource at {@code <mod-id>/config.dist.yml},
 * which is written to the config folder for admins to read. Admins put their own settings in
 * {@code config.yml}, which is merged over the defaults, so it only needs the settings they want to
 * change. As every default is expected to be in the dist file, codecs don't need any defaults of
 * their own. A mod can have other config files too, named in the same way, e.g.
 * {@code book.dist.yml} and {@code book.yml}.
 */
public final class ModConfig {
    /** The name of a mod's main config file. */
    public static final String CONFIG = "config";
    public static final String DIST_FILE = distFile(CONFIG);
    public static final String USER_FILE = userFile(CONFIG);

    private ModConfig() {}

    /**
     * Loads the mod's main config (see {@link #load(String, String, Codec)}).
     *
     * @throws ConfigException if the config can't be read or isn't valid, which stops the server
     *                         from starting
     */
    public static <T> T load(String modId, Codec<T> codec) throws ConfigException {
        return load(modId, CONFIG, codec);
    }

    /**
     * Loads one of the mod's config files, e.g. {@code book} for {@code book.yml} merged over
     * {@code book.dist.yml}, logging whether the defaults were used, and any unknown settings.
     *
     * @throws ConfigException if the config can't be read or isn't valid, which stops the server
     *                         from starting
     */
    public static <T> T load(String modId, String name, Codec<T> codec) throws ConfigException {
        final var logger = LoggerFactory.getLogger(modId);
        final var folder = FabricLoader.getInstance().getConfigDir().resolve(modId);
        final var loaded = load(folder, name, dist(modId, name), codec);

        loaded.distWriteFailure().ifPresent(e ->
            logger.warn("Couldn't update {}, so it may be out of date", folder.resolve(distFile(name)), e));

        if (loaded.usedDefaults()) {
            logger.info(
                "No {} in {}, so using the defaults. Copy {} to change them",
                userFile(name),
                folder,
                distFile(name)
            );
        }

        for (final var key : loaded.unknownKeys()) {
            logger.warn(
                "Unknown config setting '{}' in {} (a typo, or no longer used?)",
                key,
                folder.resolve(userFile(name))
            );
        }

        return loaded.config();
    }

    /**
     * @param config           The loaded config, typed.
     * @param usedDefaults     Whether there's no {@value #USER_FILE}, so the defaults were used.
     * @param unknownKeys      Settings in {@value #USER_FILE} that the mod doesn't have.
     * @param distWriteFailure Why {@value #DIST_FILE} couldn't be updated, if it couldn't.
     */
    record Loaded<T>(T config, boolean usedDefaults, List<String> unknownKeys,
                     Optional<IOException> distWriteFailure) {}

    /**
     * Writes the named config's dist file, then loads the admin's config merged over the defaults.
     */
    static <T> Loaded<T> load(Path folder, String name, String dist, Codec<T> codec) throws ConfigException {
        final var distFile = folder.resolve(distFile(name));
        final var userFile = folder.resolve(userFile(name));

        // The dist file is only there for admins to read, so failing to write it (e.g. because it's
        // been made read-only) shouldn't stop the config from loading
        final var distWriteFailure = writeIfChanged(distFile, dist);

        final var usedDefaults = !Files.exists(userFile);
        final var defaults = ConfigTree.parse(distFile(name), dist);
        final var user = usedDefaults ? Map.of() : ConfigTree.parse(userFile.toString(), read(userFile));

        final var config = codec.parse(JavaOps.INSTANCE, ConfigTree.merge(defaults, user)).getOrThrow(error ->
            new ConfigException("Invalid config in " + (usedDefaults ? distFile : userFile) + ": " + error));

        return new Loaded<>(config, usedDefaults, ConfigTree.unknownKeys(defaults, user), distWriteFailure);
    }

    /**
     * Writes the file, unless it already has exactly these contents, so its modified time only
     * changes when the expected contents do.
     */
    private static Optional<IOException> writeIfChanged(Path file, String contents) {
        if (hasContents(file, contents)) {
            return Optional.empty();
        }

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, contents);
            return Optional.empty();
        } catch (IOException e) {
            return Optional.of(e);
        }
    }

    private static boolean hasContents(Path file, String contents) {
        try {
            return Files.readString(file).equals(contents);
        } catch (IOException e) {
            // It doesn't exist, or can't be read (e.g. it's not valid UTF-8 any more), so it needs
            // writing again
            return false;
        }
    }

    private static String distFile(String name) {
        return name + ".dist.yml";
    }

    private static String userFile(String name) {
        return name + ".yml";
    }

    private static String dist(String modId, String name) {
        final var resource = modId + "/" + distFile(name);
        final var path = FabricLoader.getInstance().getModContainer(modId)
            .flatMap(container -> container.findPath(resource))
            .orElseThrow(() -> new ConfigException("The " + modId + " mod doesn't include " + resource));

        return read(path);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new ConfigException("Couldn't read " + file, e);
        }
    }
}
