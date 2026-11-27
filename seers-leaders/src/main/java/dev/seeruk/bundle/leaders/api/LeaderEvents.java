package dev.seeruk.bundle.leaders.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
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
        (listeners) -> (server, change) -> {
            for (Changed listener : listeners) {
                listener.onLeaderChanged(server, change);
            }
        }
    );

    /**
     * Fired when a period ends, with the winner of each of its categories, just before the next
     * period starts.
     */
    public static final Event<PeriodEnded> PERIOD_ENDED = EventFactory.createArrayBacked(
        PeriodEnded.class,
        (listeners) -> (server, end) -> {
            for (PeriodEnded listener : listeners) {
                listener.onPeriodEnded(server, end);
            }
        }
    );

    private LeaderEvents() {}

    /**
     * A category's leader changing.
     *
     * @param category     The category's ID.
     * @param title        The category's title, as players see it.
     * @param previous     Who led the category before, if anyone.
     * @param leader       Who leads the category now, if anyone.
     * @param announceable Whether it's worth announcing: someone took the lead from someone else,
     *                     in a category that announces changes. Leaders' own chat announcements
     *                     follow this, but it doesn't depend on whether they're turned on.
     */
    public record Change(
        String category,
        Component title,
        Optional<UUID> previous,
        Optional<UUID> leader,
        boolean announceable
    ) {}

    /**
     * A period ending.
     *
     * @param period       The period's ID.
     * @param title        The period's title, as players see it.
     * @param winners      Who won each of the period's categories, except any nobody scored in.
     * @param announceable Whether it's worth announcing: someone won something, and the server
     *                     wasn't stopped for whole periods (so the winners aren't from long ago).
     *                     Leaders' own chat announcements follow this, but it doesn't depend on
     *                     whether they're turned on.
     */
    public record PeriodEnd(String period, Component title, List<Winner> winners, boolean announceable) {}

    /**
     * Who won a category in a period.
     *
     * @param category     The category's ID.
     * @param title        The category's title, as players see it.
     * @param player       Who won it.
     * @param value        Their total for the period.
     * @param displayValue Their total as players see it, in the category's unit, e.g. "12.5" hours.
     */
    public record Winner(String category, Component title, UUID player, long value, String displayValue) {}

    @FunctionalInterface
    public interface Changed {
        /**
         * @param server The server the leader changed on
         * @param change The category, and who led it before and now
         */
        void onLeaderChanged(MinecraftServer server, Change change);
    }

    @FunctionalInterface
    public interface PeriodEnded {
        /**
         * @param server The server the period ended on
         * @param end    The period, and who won each of its categories
         */
        void onPeriodEnded(MinecraftServer server, PeriodEnd end);
    }
}
