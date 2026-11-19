package dev.seeruk.bundle.core.tick;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TickAveragesTest {
    // 20 ticks a second, each taking 10ms
    private static final long INTERVAL = 50_000_000;
    private static final long TICK = 10_000_000;

    @Test
    void shouldHaveNoAveragesBeforeAnyTicks() {
        final var averages = new TickAverages(Duration.ofMinutes(1));

        assertTrue(Double.isNaN(averages.ticksPerSecond()));
        assertTrue(Double.isNaN(averages.millisecondsPerTick()));
    }

    @Test
    void shouldAverageSteadyTicks() {
        final var averages = ticked(Duration.ofMinutes(1), 1200, TICK, INTERVAL);

        assertEquals(20, averages.ticksPerSecond(), 0.001);
        assertEquals(10, averages.millisecondsPerTick(), 0.001);
    }

    @Test
    void shouldMoveTowardsRecentTicks() {
        final var averages = ticked(Duration.ofMinutes(1), 1200, TICK, INTERVAL);

        // A minute of slow ticks (100ms each, so 10 a second) after a minute of fast ones
        for (var i = 0; i < 600; i++) {
            averages.record(100_000_000, 100_000_000);
        }

        // The slow minute counts for more than the fast one before it, but not all of it. These
        // are what the exponential weighting works out to.
        assertEquals(61.9, averages.millisecondsPerTick(), 0.1);
        assertEquals(12.7, averages.ticksPerSecond(), 0.1);
    }

    @Test
    void shouldNotLeanTowardsTheFirstTick() {
        final var averages = new TickAverages(Duration.ofMinutes(15));

        // The server catches up just after starting, so its first ticks come very close together
        averages.record(1_000_000, 1_000_000);
        for (var i = 0; i < 600; i++) {
            averages.record(TICK, INTERVAL);
        }

        // 30 seconds of steady ticks, with one quick tick that barely counts
        assertEquals(20, averages.ticksPerSecond(), 0.1);
        assertEquals(10, averages.millisecondsPerTick(), 0.1);
    }

    @Test
    void shouldMoveSlowerWithALongerWindow() {
        final var shorter = ticked(Duration.ofMinutes(1), 1200, TICK, INTERVAL);
        final var longer = ticked(Duration.ofMinutes(15), 1200, TICK, INTERVAL);

        for (var i = 0; i < 600; i++) {
            shorter.record(100_000_000, 100_000_000);
            longer.record(100_000_000, 100_000_000);
        }

        assertTrue(longer.millisecondsPerTick() < shorter.millisecondsPerTick(), "Longer window should lag");
    }

    private static TickAverages ticked(Duration window, int ticks, long tickNanos, long intervalNanos) {
        final var averages = new TickAverages(window);
        for (var i = 0; i < ticks; i++) {
            averages.record(tickNanos, intervalNanos);
        }
        return averages;
    }
}
