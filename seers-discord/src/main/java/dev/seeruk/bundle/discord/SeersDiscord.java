package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersDiscord implements ModInitializer {
    public static final String MOD_ID = "seers-discord";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, DiscordConfig.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> DiscordBot.start(config));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DiscordBot.stop());

        // Fired for messages players send with commands (e.g. /me) too
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, chat) ->
            ChatRelay.message(message, chat, sender, config.formats()).ifPresent(DiscordBot::send));

        LOGGER.info("Initialized");
    }
}
