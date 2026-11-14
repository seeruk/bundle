package dev.seeruk.bundle.nicks.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.nicks.SeersNicks.LOGGER;
import static dev.seeruk.bundle.nicks.SeersNicks.MOD_ID;

public class SeersNicksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
