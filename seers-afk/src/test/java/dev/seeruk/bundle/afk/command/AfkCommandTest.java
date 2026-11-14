package dev.seeruk.bundle.afk.command;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AfkCommandTest {
    static Stream<Arguments> shouldRecogniseAfkCommand() {
        return Stream.of(
            Arguments.of("Recognises the command", "afk", true),
            Arguments.of("Recognises the namespaced command", "seers-afk:afk", true),
            Arguments.of("Recognises the command with extra text after it", "afk brb", true),
            Arguments.of("Ignores other commands", "afkk", false),
            Arguments.of("Ignores other commands mentioning AFK", "msg Seer afk", false),
            Arguments.of("Ignores another mod's command with the same name", "other:afk", false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldRecogniseAfkCommand(final String description, final String command, final boolean expected) {
        assertEquals(expected, AfkCommand.isAfkCommand(command));
    }
}
