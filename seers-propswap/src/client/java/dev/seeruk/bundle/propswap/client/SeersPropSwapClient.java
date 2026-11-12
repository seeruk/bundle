package dev.seeruk.bundle.propswap.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.propswap.SeersPropSwap.LOGGER;
import static dev.seeruk.bundle.propswap.SeersPropSwap.MOD_ID;

public class SeersPropSwapClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
