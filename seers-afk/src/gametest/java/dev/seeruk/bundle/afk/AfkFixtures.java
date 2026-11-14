package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkEvents;
import dev.seeruk.bundle.afk.api.AfkReason;
import dev.seeruk.bundle.core.testing.EventLog;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Shared helpers for AFK's GameTests.
 */
public final class AfkFixtures {
    public record Changed(UUID id, boolean isAfk, AfkReason reason) {}

    private static final EventLog<Changed> EVENTS = new EventLog<>();

    static {
        AfkEvents.STATUS_CHANGED.register((player, isAfk, reason) ->
            EVENTS.record(new Changed(player.getUUID(), isAfk, reason)));
    }

    private AfkFixtures() {}

    @SuppressWarnings("removal")
    public static ServerPlayer player(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    /**
     * Every status change for the player, oldest first.
     */
    public static List<Changed> eventsFor(ServerPlayer player) {
        return EVENTS.matching(event -> event.id().equals(player.getUUID()));
    }
}
