package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.digest.command.DigestCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersDigest implements ModInitializer {
    public static final String MOD_ID = "seers-digest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, DigestConfig.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            DigestCommand.register(dispatcher, config));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            config.groups().forEach(group -> AllPlayerStats.unknownNames(server, group.stats()).forEach(name ->
                LOGGER.warn("Group '{}' uses '{}', which isn't a stat (yet), so it's counted as 0", group.id(), name)));
            IssueSchedule.started(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> IssueSchedule.tick(server, config));

        LOGGER.info("Initialized");
    }
}
