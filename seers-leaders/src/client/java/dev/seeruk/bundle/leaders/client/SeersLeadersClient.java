package dev.seeruk.bundle.leaders.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.leaders.SeersLeaders.LOGGER;
import static dev.seeruk.bundle.leaders.SeersLeaders.MOD_ID;

public class SeersLeadersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
