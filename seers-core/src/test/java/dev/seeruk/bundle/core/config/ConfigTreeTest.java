package dev.seeruk.bundle.core.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigTreeTest {
    @Nested
    class Parse {
        static Stream<Arguments> shouldParse() {
            return Stream.of(
                Arguments.of("An empty file has no settings", "", Map.of()),
                Arguments.of("A file with only comments has no settings", "# Nothing here yet\n", Map.of()),
                Arguments.of("Reads settings", "name: Seer\ncount: 2\n", Map.of("name", "Seer", "count", 2)),
                Arguments.of(
                    "Reads sections and lists",
                    "section:\n  enabled: true\ntags: [a, b]\n",
                    Map.of("section", Map.of("enabled", true), "tags", List.of("a", "b"))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldParse(final String description, final String yaml, final Map<?, ?> expected) {
            assertEquals(expected, ConfigTree.parse("config.yml", yaml));
        }

        static Stream<Arguments> shouldRefuse() {
            return Stream.of(
                Arguments.of("Refuses invalid YAML", "name: [Seer\n"),
                Arguments.of("Refuses duplicate settings", "name: Seer\nname: SeerUK\n"),
                Arguments.of("Refuses a file that isn't a map", "- just\n- a list\n")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRefuse(final String description, final String yaml) {
            final var e = assertThrows(ConfigException.class, () -> ConfigTree.parse("config.yml", yaml));
            assertTrue(e.getMessage().contains("config.yml"), "Error doesn't say which file: " + e.getMessage());
        }
    }

    @Nested
    class Merge {
        private static final Map<String, Object> DEFAULTS = Map.of(
            "name", "default",
            "section", Map.of("enabled", false, "ratio", 0.5),
            "tags", List.of("a", "b")
        );

        static Stream<Arguments> shouldMerge() {
            return Stream.of(
                Arguments.of("Uses the defaults without any overrides", Map.of(), DEFAULTS),
                Arguments.of(
                    "Overrides a setting",
                    Map.of("name", "Seer"),
                    Map.of("name", "Seer", "section", DEFAULTS.get("section"), "tags", DEFAULTS.get("tags"))
                ),
                Arguments.of(
                    "Overrides a setting in a section, keeping the rest of the section",
                    Map.of("section", Map.of("enabled", true)),
                    Map.of("name", "default", "section", Map.of("enabled", true, "ratio", 0.5), "tags", DEFAULTS.get("tags"))
                ),
                Arguments.of(
                    "Replaces a list whole",
                    Map.of("tags", List.of("c")),
                    Map.of("name", "default", "section", DEFAULTS.get("section"), "tags", List.of("c"))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldMerge(final String description, final Map<?, ?> overrides, final Map<?, ?> expected) {
            assertEquals(expected, ConfigTree.merge(DEFAULTS, overrides));
        }

        @Test
        void emptySettingKeepsItsDefault() {
            assertEquals(DEFAULTS, ConfigTree.merge(DEFAULTS, ConfigTree.parse("config.yml", "name:\n")));
        }
    }

    @Nested
    class UnknownKeys {
        private static final Map<String, Object> DEFAULTS = Map.of(
            "name", "default",
            "section", Map.of("enabled", false),
            "tags", List.of("a")
        );

        static Stream<Arguments> shouldFindUnknownKeys() {
            return Stream.of(
                Arguments.of("Finds nothing without any overrides", Map.of(), List.of()),
                Arguments.of("Finds nothing when every setting is known", Map.of("name", "Seer", "section", Map.of("enabled", true)), List.of()),
                Arguments.of("Finds an unknown setting", Map.of("nmae", "Seer"), List.of("nmae")),
                Arguments.of("Finds an unknown setting in a section", Map.of("section", Map.of("enabld", true)), List.of("section.enabld")),
                Arguments.of("Finds an unknown section", Map.of("other", Map.of("enabled", true)), List.of("other")),
                Arguments.of("Doesn't look inside a setting that should be a list", Map.of("tags", Map.of("a", 1)), List.of())
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldFindUnknownKeys(final String description, final Map<?, ?> overrides, final List<String> expected) {
            assertEquals(expected, ConfigTree.unknownKeys(DEFAULTS, overrides));
        }
    }
}
