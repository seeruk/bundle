package dev.seeruk.bundle.nicks;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NicknameTest {
    private static final Predicate<String> TAKEN = name -> name.equalsIgnoreCase("Taken");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Validate {
        static Stream<Arguments> shouldAllow() {
            return Stream.of(
                Arguments.of("Allows a short nickname", Component.literal("Seer"), false),
                Arguments.of("Allows the maximum length", Component.literal("a".repeat(Nickname.MAX_LENGTH)), false),
                Arguments.of(
                    "Styling doesn't count towards the length",
                    Component.literal("a".repeat(Nickname.MAX_LENGTH)).withStyle(ChatFormatting.RED),
                    false
                ),
                Arguments.of("Allows admins to use other players' usernames", Component.literal("Taken"), true)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldAllow(final String description, final Component name, final boolean trusted) {
            assertEquals(Optional.empty(), Nickname.validate(name, trusted, TAKEN));
        }

        static Stream<Arguments> shouldRefuse() {
            final var tooLong = Component.literal("a".repeat(Nickname.MAX_LENGTH + 1));
            return Stream.of(
                Arguments.of("Refuses an empty nickname", Component.empty(), new NickEditResult.BlankName()),
                Arguments.of("Refuses a blank nickname", Component.literal("   "), new NickEditResult.BlankName()),
                Arguments.of(
                    "Refuses a nickname over the maximum length",
                    tooLong,
                    new NickEditResult.MaxLengthExceeded(Nickname.MAX_LENGTH, tooLong)
                ),
                Arguments.of(
                    "Refuses another player's username",
                    Component.literal("Taken"),
                    new NickEditResult.UsernameTaken("Taken")
                ),
                Arguments.of(
                    "Refuses another player's username in a different case",
                    Component.literal("TAKEN"),
                    new NickEditResult.UsernameTaken("TAKEN")
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRefuse(final String description, final Component name, final NickEditResult.Failure expected) {
            assertEquals(Optional.of(expected), Nickname.validate(name, false, TAKEN));
        }
    }
}
