package dev.seeruk.bundle.chat;

import dev.seeruk.bundle.core.testing.EventLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared helpers for Chat's GameTests. Starts recording sent messages when the game starts, so
 * every test's messages are recorded.
 */
public final class ChatFixtures implements ModInitializer {
    public record Sent(UUID sender, PlayerChatMessage message, ResourceKey<ChatType> chatType) {}

    private static final EventLog<Sent> SENT = new EventLog<>();

    @Override
    public void onInitialize() {
        // Fired for messages players send with commands (e.g. /me) too
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, chatType) ->
            SENT.record(new Sent(sender.getUUID(), message, chatType.chatType().unwrapKey().orElseThrow())));
    }

    /**
     * Every message the player has sent, oldest first.
     */
    public static List<Sent> sentBy(ServerPlayer player) {
        return SENT.matching(sent -> sent.sender().equals(player.getUUID()));
    }

    /**
     * The only message the player has sent, failing the test if there isn't exactly one.
     */
    public static Sent onlySentBy(GameTestHelper helper, ServerPlayer player) {
        final var sent = sentBy(player);
        helper.assertValueEqual(sent.size(), 1, "messages sent");
        return sent.getFirst();
    }

    /**
     * The style of each piece of text in the component.
     */
    public static List<Style> styles(Component component) {
        final var styles = new ArrayList<Style>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return styles;
    }

    /**
     * Sends a chat message from the player, the way a real client would. It's unsigned, since the
     * GameTest server doesn't enforce secure chat.
     */
    public static void chat(ServerPlayer player, String message) {
        final var lastSeen = new LastSeenMessages.Update(0, new BitSet(), LastSeenMessages.Update.IGNORE_CHECKSUM);
        player.connection.handleChat(new ServerboundChatPacket(message, Instant.now(), 0L, Optional.empty(), lastSeen));
    }
}
