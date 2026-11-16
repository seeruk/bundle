package dev.seeruk.bundle.core.config;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Config as YAML reads it: maps of settings, lists, and plain values like strings and numbers.
 */
final class ConfigTree {
    private ConfigTree() {}

    /**
     * Parses a YAML config file. An empty file (or one with only comments) has no settings.
     *
     * @param label What's being parsed, e.g. the file name, for error messages.
     * @param yaml  The raw YAML to parse.
     */
    static Map<?, ?> parse(String label, String yaml) {
        final Object parsed;
        try {
            parsed = new Load(LoadSettings.builder().setLabel(label).build()).loadFromString(yaml);
        } catch (YamlEngineException e) {
            throw new ConfigException("Couldn't read " + label + ": " + e.getMessage(), e);
        }

        return switch (parsed) {
            case null -> Map.of();
            case Map<?, ?> map -> map;
            default -> throw new ConfigException(label + " should contain settings, e.g. 'setting: value'");
        };
    }

    /**
     * Merges {@code overrides} over {@code defaults}, section by section. Anything else (e.g. a
     * list) is replaced whole, and an empty setting keeps its default.
     */
    static Map<Object, Object> merge(Map<?, ?> defaults, Map<?, ?> overrides) {
        final var merged = new LinkedHashMap<Object, Object>(defaults);
        overrides.forEach((key, value) -> {
            if (value != null) {
                merged.merge(
                    key, value, (original, override) ->
                        original instanceof Map<?, ?> section && override instanceof Map<?, ?> overrideSection
                            ? merge(section, overrideSection)
                            : override
                );
            }
        });
        return merged;
    }

    /**
     * Lists every setting in {@code overrides} that's not in {@code defaults}, as dotted paths
     * (e.g. {@code afk.timout}).
     */
    static List<String> unknownKeys(Map<?, ?> defaults, Map<?, ?> overrides) {
        final var unknown = new ArrayList<String>();
        collectUnknownKeys("", defaults, overrides, unknown);
        return unknown;
    }

    private static void collectUnknownKeys(String path, Map<?, ?> defaults, Map<?, ?> overrides, List<String> unknown) {
        overrides.forEach((key, value) -> {
            final var keyPath = path + key;
            if (!defaults.containsKey(key)) {
                unknown.add(keyPath);
            } else if (defaults.get(key) instanceof Map<?, ?> section && value instanceof Map<?, ?> overrideSection) {
                collectUnknownKeys(keyPath + ".", section, overrideSection, unknown);
            }
        });
    }
}
