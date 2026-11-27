package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.leaders.command.LeadersCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

public class SeersLeaders implements ModInitializer {
    public static final String MOD_ID = "seers-leaders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, LeadersConfig.CODEC);
        final var clock = Clock.system(config.timezone());

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            LeadersCommand.register(dispatcher, config));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            config.categories().forEach(category -> AllPlayerStats.unknownNames(
                server,
                category.stats()
            ).forEach(name ->
                LOGGER.warn(
                    "Category '{}' uses '{}', which isn't a stat (yet), so it's counted as 0",
                    category.id(),
                    name
                )));
            Leaders.forgetRemovedResults(server, config);
            Periods.forgetRemoved(server, config);
            Leaders.refresh(server, config);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Periods are checked every second, so they end within a second of their schedule.
            // They're checked from ticks, rather than on start-up, so a period that ended while the
            // server was stopped only ends once every mod has started, and can listen for it.
            if (server.getTickCount() % SharedConstants.TICKS_PER_SECOND == 0) {
                Periods.check(server, config, clock);
            }
            // Boards are refreshed less often, as reading every player's stats costs more
            if (server.getTickCount() % (config.refreshSeconds() * SharedConstants.TICKS_PER_SECOND) == 0) {
                Leaders.refresh(server, config);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Leaders.clear());

        LeaderMannequins.init();
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> LeaderMannequins.track(entity, config));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> LeaderMannequins.untrack(entity));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LeaderMannequins.clear());

        LOGGER.info("Initialized");
    }
}
