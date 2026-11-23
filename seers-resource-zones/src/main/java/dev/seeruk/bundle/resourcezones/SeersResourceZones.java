package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents;
import dev.seeruk.bundle.resourcezones.command.ResourceZonesCommand;
import dev.seeruk.bundle.resourcezones.export.ZoneExports;
import dev.seeruk.bundle.resourcezones.particles.ZoneParticles;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SeersResourceZones implements ModInitializer {
    public static final String MOD_ID = "seers-resource-zones";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, ResourceZonesConfig.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            ResourceZonesCommand.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> ZoneParticles.showAll(server, config.particles()));

        final var notice = config.notice().format(Map.of());
        ActionBarMessages.register(id("notice"), player -> ZoneNotice.message(player, notice));

        // Handle zone exports
        final var exportFolder = config.export().folder();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ZoneExports.exportAll(server, exportFolder));
        ResourceZoneEvents.CHANGED.register((server, dimension) -> ZoneExports.export(server, exportFolder, dimension));

        LOGGER.info("Initialized");
    }
}
