package dev.seeruk.bundle.resourcezones.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.resourcezones.SeersResourceZones.LOGGER;
import static dev.seeruk.bundle.resourcezones.SeersResourceZones.MOD_ID;

public class SeersResourceZonesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
