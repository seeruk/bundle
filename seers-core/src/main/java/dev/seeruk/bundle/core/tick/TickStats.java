package dev.seeruk.bundle.core.tick;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.time.Duration;

/**
 * How fast the server's been ticking, averaged over the last 1, 5, and 15 minutes. Vanilla only
 * keeps the last 100 ticks (about 5 seconds), which is shorter than what we're after.
 *
 * <p>Ticks are measured from start to end, and from the end of one to the end of the next, so
 * it's only used on the server thread.
 */
public final class TickStats {
    /**
     * The possible windows of time that ticks are averaged over.
     */
    public enum Window {
        ONE_MINUTE(Duration.ofMinutes(1)),
        FIVE_MINUTES(Duration.ofMinutes(5)),
        FIFTEEN_MINUTES(Duration.ofMinutes(15));

        private final Duration duration;

        Window(Duration duration) {
            this.duration = duration;
        }
    }

    // Longer than this between ticks means the server wasn't ticking (e.g. it paused while empty),
    // rather than ticking slowly, so it's not counted as an interval
    private static final long PAUSE_NANOS = Duration.ofSeconds(10).toNanos();

    private static TickAverages[] averages = averages();
    private static long tickStart;
    private static long lastTickEnd;

    private TickStats() {}

    /**
     * Starts measuring ticks. Should only be called once. It may already be registered by loading
     * this mod.
     */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            averages = averages();
            lastTickEnd = 0;
        });
        ServerTickEvents.START_SERVER_TICK.register(server -> tickStart = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server -> ended(System.nanoTime()));
    }

    /**
     * Returns how many ticks have been happening a second, on average, over the window, or NaN if
     * there haven't been two ticks yet.
     */
    public static double ticksPerSecond(Window window) {
        return averages[window.ordinal()].ticksPerSecond();
    }

    /**
     * Returns how many milliseconds ticks have been taking, on average, over the window, or NaN if
     * there haven't been two ticks yet.
     */
    public static double millisecondsPerTick(Window window) {
        return averages[window.ordinal()].millisecondsPerTick();
    }

    private static void ended(long now) {
        final var interval = now - lastTickEnd;
        if (lastTickEnd != 0 && interval < PAUSE_NANOS) {
            for (final var average : averages) {
                average.record(now - tickStart, interval);
            }
        }
        lastTickEnd = now;
    }

    private static TickAverages[] averages() {
        final var windows = Window.values();
        final var averages = new TickAverages[windows.length];
        for (var i = 0; i < windows.length; i++) {
            averages[i] = new TickAverages(windows[i].duration);
        }
        return averages;
    }
}
