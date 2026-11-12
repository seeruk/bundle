package dev.seeruk.bundle.propswap;

import dev.seeruk.bundle.propswap.api.PropSwapEvents;
import dev.seeruk.bundle.propswap.command.PropSwapCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersPropSwap implements ModInitializer {
    public static final String MOD_ID = "seers-propswap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Runs after the default phase, so claim mods (etc.) can deny an interaction first.
     */
    public static final Identifier LATE_PHASE = id("late");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            PropSwapCommand.register(dispatcher));

        UseBlockCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
        UseBlockCallback.EVENT.register(LATE_PHASE, PropSwapInteraction::interact);

        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) ->
            PropSwapMode.disable(listener.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PropSwapMode.clear());

        ServerLifecycleEvents.SERVER_STARTED.register(PropSwapStats::ensureObjectives);
        PropSwapEvents.SWAPPED.register(PropSwapStats::recordSwapped);

        LOGGER.info("Initialized");
    }
}
