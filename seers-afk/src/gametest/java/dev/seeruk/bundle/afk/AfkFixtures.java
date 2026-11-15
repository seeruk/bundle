package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkEvents;
import dev.seeruk.bundle.afk.api.AfkReason;
import dev.seeruk.bundle.core.testing.EventLog;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Shared helpers for AFK's GameTests. Starts recording AFK status changes when the game starts, so
 * every test's changes are recorded.
 */
public final class AfkFixtures implements ModInitializer {
    public record Changed(UUID id, boolean isAfk, AfkReason reason) {}

    private static final EventLog<Changed> EVENTS = new EventLog<>();

    @Override
    public void onInitialize() {
        AfkEvents.STATUS_CHANGED.register((player, isAfk, reason) ->
            EVENTS.record(new Changed(player.getUUID(), isAfk, reason)));
    }

    /**
     * Every status change for the player, oldest first.
     */
    public static List<Changed> eventsFor(ServerPlayer player) {
        return EVENTS.matching(event -> event.id().equals(player.getUUID()));
    }
}
