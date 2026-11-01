package dev.seeruk.bundle.template.client;

import net.fabricmc.api.ClientModInitializer;
import static dev.seeruk.bundle.template.SeersTemplate.LOGGER;
import static dev.seeruk.bundle.template.SeersTemplate.MOD_ID;

public class SeersTemplateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
