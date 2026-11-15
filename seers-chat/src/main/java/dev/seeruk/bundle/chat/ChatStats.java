package dev.seeruk.bundle.chat;

import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static dev.seeruk.bundle.chat.SeersChat.MOD_ID;

/**
 * Keeps per-player chat statistics in Vanilla scoreboard objectives (see {@link ScoreboardStat}).
 */
public final class ChatStats {
    public static final String MESSAGES_SENT_OBJECTIVE = "messages_sent";
    public static final String MESSAGE_CHARACTERS_OBJECTIVE = "message_characters";
    public static final String EMOTES_SENT_OBJECTIVE = "emotes_sent";
    public static final String EMOTE_CHARACTERS_OBJECTIVE = "emote_characters";

    /**
     * Total number of chat messages each player has sent.
     */
    static final ScoreboardStat MESSAGES_SENT = new ScoreboardStat(MOD_ID, MESSAGES_SENT_OBJECTIVE, "Messages Sent");

    /**
     * Total number of characters in the chat messages each player has sent.
     */
    static final ScoreboardStat MESSAGE_CHARACTERS = new ScoreboardStat(MOD_ID, MESSAGE_CHARACTERS_OBJECTIVE, "Message Characters Sent");

    /**
     * Total number of emotes ({@code /me}) each player has sent.
     */
    static final ScoreboardStat EMOTES_SENT = new ScoreboardStat(MOD_ID, EMOTES_SENT_OBJECTIVE, "Emotes Sent");

    /**
     * Total number of characters in the emotes each player has sent.
     */
    static final ScoreboardStat EMOTE_CHARACTERS = new ScoreboardStat(MOD_ID, EMOTE_CHARACTERS_OBJECTIVE, "Emote Characters Sent");

    private ChatStats() {}

    /**
     * Creates any missing objectives. Safe to call more than once.
     */
    public static void ensureObjectives(MinecraftServer server) {
        MESSAGES_SENT.ensureObjective(server);
        MESSAGE_CHARACTERS.ensureObjective(server);
        EMOTES_SENT.ensureObjective(server);
        EMOTE_CHARACTERS.ensureObjective(server);
    }

    /**
     * Adds one to the player's chat messages or emotes sent, and the message's length to their
     * characters. The length is of the message as shown, so formatting tags don't count. Other
     * kinds of message (e.g. {@code /say}) don't count.
     *
     * @param player   The player who sent the message.
     * @param message  The message.
     * @param chatType The kind of message, e.g. chat or an emote.
     */
    public static void recordMessageSent(ServerPlayer player, PlayerChatMessage message, ChatType.Bound chatType) {
        if (chatType.chatType().is(ChatType.CHAT)) {
            MESSAGES_SENT.add(player, 1);
            MESSAGE_CHARACTERS.add(player, length(message));
        } else if (chatType.chatType().is(ChatType.EMOTE_COMMAND)) {
            EMOTES_SENT.add(player, 1);
            EMOTE_CHARACTERS.add(player, length(message));
        }
    }

    private static int length(PlayerChatMessage message) {
        return message.decoratedContent().getString().length();
    }
}
