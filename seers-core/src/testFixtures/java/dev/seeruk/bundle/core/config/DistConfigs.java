package dev.seeruk.bundle.core.config;

import com.mojang.serialization.Codec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Reads a mod's shipped default config files in tests, the same way the game reads them, so a
 * mistake in one fails a test rather than stopping the game from starting.
 */
public final class DistConfigs {
    private DistConfigs() {}

    /**
     * Reads the mod's {@code <name>.dist.yml}, e.g. {@code config} for its main config.
     */
    public static <T> T read(String modId, String name, Codec<T> codec) {
        final var resource = modId + "/" + name + ".dist.yml";
        final String yaml;
        try (final var stream = DistConfigs.class.getClassLoader().getResourceAsStream(resource)) {
            yaml = new String(Objects.requireNonNull(stream, resource + " isn't on the classpath").readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Couldn't read " + resource, e);
        }

        return TestConfigs.parse(resource, yaml, codec);
    }
}
