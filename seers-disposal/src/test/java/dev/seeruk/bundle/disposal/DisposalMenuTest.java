package dev.seeruk.bundle.disposal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DisposalMenuTest {
    @Nested
    class DisposedMessage {
        @Test
        void shouldReturnSingularMessage() {
            final var message = DisposalMenu.disposedMessage(1);

            assertEquals("Disposed of 1 item", message);

        }

        @Test
        void shouldReturnPluralMessage() {
            final var message = DisposalMenu.disposedMessage(2);

            assertEquals("Disposed of 2 items", message);
        }
    }
}
