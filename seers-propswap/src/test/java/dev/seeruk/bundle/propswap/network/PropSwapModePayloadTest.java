package dev.seeruk.bundle.propswap.network;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropSwapModePayloadTest {
    @ParameterizedTest(name = "Round trips enabled = {0}")
    @ValueSource(booleans = {true, false})
    void shouldRoundTrip(final boolean enabled) {
        final var buffer = Unpooled.buffer();

        PropSwapModePayload.CODEC.encode(buffer, new PropSwapModePayload(enabled));

        assertEquals(new PropSwapModePayload(enabled), PropSwapModePayload.CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }
}
