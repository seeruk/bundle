package dev.seeruk.bundle.core;

/**
 * Represents the result of an operation that is expected to be able to succeed or fail.
 *
 * @param <S> The type of the success value.
 * @param <E> The type of the error value.
 */
public sealed interface Result<S, E> {
    record Success<S, E>(S value) implements Result<S, E> {}

    record Failure<S, E>(E error) implements Result<S, E> {}
}
