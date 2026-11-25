package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.leaders.api.LeaderEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersLeaders implements ModInitializer {
    public static final String MOD_ID = "seers-leaders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, LeadersConfig.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> Leaders.started(server, config));
        ServerTickEvents.END_SERVER_TICK.register(server -> Leaders.tick(server, config));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Leaders.clear());

        LeaderEvents.CHANGED.register((server, category, previous, leader) ->
            LeaderAnnouncements.announce(server, config, category, leader));

        LOGGER.info("Initialized");
    }
}
