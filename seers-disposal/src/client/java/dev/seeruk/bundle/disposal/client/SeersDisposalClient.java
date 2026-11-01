package dev.seeruk.bundle.disposal.client;

import net.fabricmc.api.ClientModInitializer;
import static dev.seeruk.bundle.disposal.SeersDisposal.LOGGER;
import static dev.seeruk.bundle.disposal.SeersDisposal.MOD_ID;

public class SeersDisposalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
