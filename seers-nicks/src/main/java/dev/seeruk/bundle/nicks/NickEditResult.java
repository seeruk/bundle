package dev.seeruk.bundle.nicks;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * NickEditResult is a result type used to represent the outcome of attempting to edit a player's
 * nickname.
 */
public sealed interface NickEditResult {
    // Success cases

    /**
     * A {@link NickEditResult} returned when the nickname was successfully edited. A new nickname
     * being accepted despite being identical to the current one also counts as successful.
     *
     * @param original The original nickname, if there was one.
     * @param edited   The edited nickname, if there is one now.
     */
    record Edited(Optional<Component> original, Optional<Component> edited) implements NickEditResult {}

    // Failure cases
    sealed interface Failure extends NickEditResult {}

    /**
     * A {@link Failure} returned when a provided nickname is blank.
     */
    record BlankName() implements Failure {}

    /**
     * A {@link Failure} returned when a provided nickname starts or ends with whitespace.
     */
    record SurroundingWhitespace() implements Failure {}

    /**
     * A {@link Failure} returned when a provided nickname's length is invalid.
     *
     * @param maxNameLength The maximum nickname length.
     * @param name          The nickname that has an invalid length.
     */
    record MaxLengthExceeded(int maxNameLength, Component name) implements Failure {}

    /**
     * A {@link Failure} returned when a provided nickname is another player's username.
     *
     * @param username The username it matches.
     */
    record UsernameTaken(String username) implements Failure {}

    /**
     * A {@link Failure} returned when a provided nickname is another player's nickname.
     *
     * @param nickname The nickname it matches.
     */
    record NicknameTaken(String nickname) implements Failure {}

    /**
     * A {@link Failure} returned when the player does not have a nickname.
     */
    record NoNickname() implements Failure {}
}
