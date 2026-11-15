package dev.seeruk.bundle.chat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeersChat implements ModInitializer {
    public static final String MOD_ID = "seers-chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, MessageFormatter::format);

        ServerLifecycleEvents.SERVER_STARTED.register(ChatStats::ensureObjectives);
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, chatType) ->
            ChatStats.recordMessageSent(sender, message, chatType));

        LOGGER.info("Initialized");
    }
}
