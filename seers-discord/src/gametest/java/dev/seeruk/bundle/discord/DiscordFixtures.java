package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.testing.EventLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Shared helpers for Discord's GameTests. Starts recording the game's system messages when the
 * game starts, so every test's messages are recorded.
 */
public final class DiscordFixtures implements ModInitializer {
    private static final EventLog<Component> GAME_MESSAGES = new EventLog<>();

    @Override
    public void onInitialize() {
        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> GAME_MESSAGES.record(message));
    }

    /**
     * Every system message shown to players that contains the text, oldest first.
     */
    public static List<Component> gameMessagesContaining(String text) {
        return GAME_MESSAGES.matching(message -> message.getString().contains(text));
    }
}
