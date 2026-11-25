package dev.seeruk.bundle.leaders.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public leaders events API.
 */
public final class LeaderEvents {
    /**
     * Fired when a category's leader (who holds first place) changes, including to nobody when a
     * period starts again. Not fired the first time a category's leader is worked out, as there's
     * nobody to compare with.
     */
    public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(
        Changed.class,
        (listeners) -> (server, category, previous, leader) -> {
            for (Changed listener : listeners) {
                listener.onLeaderChanged(server, category, previous, leader);
            }
        }
    );

    /**
     * Fired when a period ends, with the winner of each of its categories, just before the next
     * period starts.
     */
    public static final Event<PeriodEnded> PERIOD_ENDED = EventFactory.createArrayBacked(
        PeriodEnded.class,
        (listeners) -> (server, period, winners) -> {
            for (PeriodEnded listener : listeners) {
                listener.onPeriodEnded(server, period, winners);
            }
        }
    );

    private LeaderEvents() {}

    /**
     * Who won a category in a period.
     *
     * @param category The category's ID.
     * @param player   Who won it.
     * @param value    Their total for the period.
     */
    public record Winner(String category, UUID player, long value) {}

    @FunctionalInterface
    public interface Changed {
        /**
         * @param server   The server the leader changed on
         * @param category The category's ID
         * @param previous Who led the category before, if anyone
         * @param leader   Who leads the category now, if anyone
         */
        void onLeaderChanged(MinecraftServer server, String category, Optional<UUID> previous, Optional<UUID> leader);
    }

    @FunctionalInterface
    public interface PeriodEnded {
        /**
         * @param server  The server the period ended on
         * @param period  The period's ID
         * @param winners Who won each of the period's categories, leaving out any nobody scored in
         */
        void onPeriodEnded(MinecraftServer server, String period, List<Winner> winners);
    }
}
