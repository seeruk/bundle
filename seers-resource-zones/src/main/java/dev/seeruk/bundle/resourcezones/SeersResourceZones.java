package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.resourcezones.command.ResourceZonesCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersResourceZones implements ModInitializer {
    public static final String MOD_ID = "seers-resource-zones";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            ResourceZonesCommand.register(dispatcher));

        LOGGER.info("Initialized");
    }
}
