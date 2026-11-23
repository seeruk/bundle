package dev.seeruk.bundle.core;

import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import dev.seeruk.bundle.core.tick.TickStats;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersCore implements ModInitializer {
    public static final String MOD_ID = "seers-core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        TickStats.register();

        ServerTickEvents.END_SERVER_TICK.register(ActionBarMessages::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ActionBarMessages.forget(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ActionBarMessages.clear());

        LOGGER.info("Initialized");
    }
}
