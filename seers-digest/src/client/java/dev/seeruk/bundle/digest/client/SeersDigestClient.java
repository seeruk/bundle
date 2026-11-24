package dev.seeruk.bundle.digest.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.digest.SeersDigest.LOGGER;
import static dev.seeruk.bundle.digest.SeersDigest.MOD_ID;

public class SeersDigestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
