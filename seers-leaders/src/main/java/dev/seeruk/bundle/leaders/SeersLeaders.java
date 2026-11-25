package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.leaders.command.LeadersCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
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

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            LeadersCommand.register(dispatcher, config));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> Leaders.started(server, config));
        ServerTickEvents.END_SERVER_TICK.register(server -> Leaders.tick(server, config));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Leaders.clear());


        LeaderMannequins.init();
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> LeaderMannequins.loaded(entity, config));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> LeaderMannequins.unloaded(entity));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LeaderMannequins.clear());

        LOGGER.info("Initialized");
    }
}
