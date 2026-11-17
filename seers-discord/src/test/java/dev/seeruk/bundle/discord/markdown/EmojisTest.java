package dev.seeruk.bundle.discord.markdown;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmojisTest {
    static Stream<Arguments> shouldNameEmojiTheGameCantShow() {
        return Stream.of(
            Arguments.of("Names an emoji", "Hi 😄", "Hi :smile:"),
            Arguments.of("Uses Discord's name", "🤔", ":thinking:"),
            Arguments.of("Names an emoji with a skin tone", "👍🏽", ":thumbsup_tone3:"),
            Arguments.of("Names an emoji made of others", "👨‍👩‍👧", ":family_mwg:"),
            Arguments.of("Names several emoji", "😄🤔", ":smile::thinking:")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldNameEmojiTheGameCantShow(final String description, final String text, final String expected) {
        assertEquals(expected, Emojis.named(text));
    }

    static Stream<Arguments> shouldLeaveWhatTheGameCanShow() {
        return Stream.of(
            Arguments.of("Leaves text without emoji alone", "Hello World!", "Hello World!"),
            Arguments.of("Leaves emoji the game can show", "I ❤ it ©", "I ❤ it ©"),
            Arguments.of("Leaves emoji the game can show, without asking for colour", "❤\uFE0F", "❤")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldLeaveWhatTheGameCanShow(final String description, final String text, final String expected) {
        assertEquals(expected, Emojis.named(text));
    }
}
