package dev.seeruk.bundle.discord.client;

import net.fabricmc.api.ClientModInitializer;

import static dev.seeruk.bundle.discord.SeersDiscord.LOGGER;
import static dev.seeruk.bundle.discord.SeersDiscord.MOD_ID;

public class SeersDiscordClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello, World! From {} client", MOD_ID);
    }
}
