package dev.seeruk.bundle.chat.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.chat.SeersChat.LOGGER;
import static dev.seeruk.bundle.chat.SeersChat.MOD_ID;

public class SeersChatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
