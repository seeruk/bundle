package dev.seeruk.bundle.text.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.text.SeersText.LOGGER;
import static dev.seeruk.bundle.text.SeersText.MOD_ID;

public class SeersTextClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
