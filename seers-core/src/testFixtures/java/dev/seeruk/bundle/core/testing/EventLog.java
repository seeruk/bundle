package dev.seeruk.bundle.core.testing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * A utility for logging events that are fired during tests, making them more easily available to
 * test code with a small and understandable API.
 *
 * <p>Why though? Because Fabric event listeners can't be removed once registered. This allows the
 * {@code EventLog} to be used as the only listener for a Fabric event in a test class, and then
 * filtered in each test case to only include events fired by that test.
 *
 * <p>A quick note on usage, players may share a name, but you can compare them with {@code ==} to
 * avoid running into issues (i.e. same player instance instead).
 *
 * @param <T> The type of logged event, usually a record of the listener's arguments. See usage.
 */
public final class EventLog<T> {
    private final List<T> events = new CopyOnWriteArrayList<>();

    public void record(T event) {
        events.add(event);
    }

    /**
     * Marks the current point in the log, so {@link #matchingSince(int, Predicate)} can skip
     * anything recorded before it (e.g. events fired by a test's setup).
     */
    public int mark() {
        return events.size();
    }

    public List<T> matching(Predicate<T> filter) {
        return matchingSince(0, filter);
    }

    public List<T> matchingSince(int mark, Predicate<T> filter) {
        return events.stream().skip(mark).filter(filter).toList();
    }
}
