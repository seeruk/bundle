package dev.seeruk.bundle.itemtext.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.itemtext.SeersItemText.LOGGER;
import static dev.seeruk.bundle.itemtext.SeersItemText.MOD_ID;

public class SeersItemTextClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
