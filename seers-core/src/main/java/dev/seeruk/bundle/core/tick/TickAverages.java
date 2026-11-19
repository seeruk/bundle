package dev.seeruk.bundle.core.tick;

import java.time.Duration;

/**
 * Rolling averages of how fast the server ticks, over a window of time. Each average is
 * exponentially weighted, like Linux's load averages: older ticks count for less and less, and a
 * tick a whole window ago counts for about a third of one now. So it needs no list of past ticks.
 *
 * <p>It keeps decaying totals (of ticks, time, and how long ticks took), rather than an average
 * that's moved towards each tick. That way, before a whole window has passed, it's the average of
 * every tick so far, rather than leaning towards whichever tick came first.
 */
public final class TickAverages {
    private final double windowNanos;

    // Decaying totals: how many ticks there have been, how long they've been happening over, and
    // how long they took
    private double ticks;
    private double intervalNanos;
    private double tickNanos;

    public TickAverages(Duration window) {
        this.windowNanos = window.toNanos();
    }

    /**
     * Records a tick.
     *
     * @param tickNanos     How long the tick took.
     * @param intervalNanos How long it's been since the tick before it ended, i.e. how often ticks
     *                      are happening.
     */
    public void record(long tickNanos, long intervalNanos) {
        // Everything before this tick counts for a little less, by how much time has passed
        final var decay = Math.exp(-intervalNanos / windowNanos);
        this.ticks = this.ticks * decay + 1;
        this.intervalNanos = this.intervalNanos * decay + intervalNanos;
        this.tickNanos = this.tickNanos * decay + tickNanos;
    }

    /**
     * Returns how many ticks are happening a second, or NaN if there haven't been any yet.
     */
    public double ticksPerSecond() {
        return ticks / intervalNanos * 1_000_000_000;
    }

    /**
     * Returns how many milliseconds ticks are taking, or NaN if there haven't been any yet.
     */
    public double millisecondsPerTick() {
        return tickNanos / ticks / 1_000_000;
    }
}
