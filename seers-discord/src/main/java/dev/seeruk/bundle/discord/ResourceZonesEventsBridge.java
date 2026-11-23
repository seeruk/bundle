package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.config.EventsConfig;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;

/**
 * Sends resource zones being reset to Discord. Seer's Resource Zones is optional, so this is its own
 * class, which is only loaded (and so only needs the Resource Zones mod) if that mod is.
 */
final class ResourceZonesEventsBridge {
    private ResourceZonesEventsBridge() {}

    static void register(EventsConfig events) {
        ResourceZoneEvents.RESET.register((server, chunks) ->
            DiscordRelay.resourceZonesReset(chunks, events).ifPresent(DiscordBot::send));
    }
}
