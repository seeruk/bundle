package dev.seeruk.bundle.core.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;

/**
 * Reads config written as YAML in tests, the same way the game reads config files, so tests can
 * write config the way admins do.
 */
public final class TestConfigs {
    private TestConfigs() {}

    /**
     * Reads the YAML as the codec's config, failing the test if it isn't valid.
     */
    public static <T> T parse(String yaml, Codec<T> codec) {
        return parse("test config", yaml, codec);
    }

    static <T> T parse(String label, String yaml, Codec<T> codec) {
        return codec.parse(JavaOps.INSTANCE, ConfigTree.parse(label, yaml))
            .getOrThrow(error -> new AssertionError("Invalid config in " + label + ": " + error));
    }
}
