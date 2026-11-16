package dev.seeruk.bundle.discord;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiscordTemplateTest {
    static Stream<Arguments> shouldFillInPlaceholders() {
        return Stream.of(
            Arguments.of(
                "Fills in placeholders",
                "_${name} ${message}_",
                Map.of("name", "Seer", "message", "waves"),
                "_Seer waves_"
            ),
            Arguments.of(
                "Leaves placeholders without a value as written",
                "${name} ${other}",
                Map.of("name", "Seer"),
                "Seer ${other}"
            ),
            Arguments.of(
                "Doesn't fill in placeholders in values",
                "${name}: ${message}",
                Map.of("name", "Seer", "message", "${name}"),
                "Seer: ${name}"
            ),
            Arguments.of(
                "Puts in values with special characters as they are",
                "${message}",
                Map.of("message", "$1 \\o/"),
                "$1 \\o/"
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldFillInPlaceholders(final String description, final String template, final Map<String, String> values, final String expected) {
        assertEquals(expected, new DiscordTemplate(template).format(values));
    }
}
