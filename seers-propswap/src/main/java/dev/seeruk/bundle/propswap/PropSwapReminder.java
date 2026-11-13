package dev.seeruk.bundle.propswap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps a "PropSwap enabled" reminder in the action bar while a player has PropSwap on and is
 * holding a tool it works with. It backs off for a few seconds whenever any other action bar
 * message is sent to the player, so it never hides anything else.
 */
public final class PropSwapReminder {
    // Action bar text fades after about 3 seconds, so this refreshes it a bit before that
    static final int REFRESH_TICKS = 40;
    static final int BACK_OFF_TICKS = 80;

    private static final Component MESSAGE = Component.literal("PropSwap enabled").withStyle(ChatFormatting.GRAY);

    // Packets can be sent from other threads (e.g. by other mods)
    private static final Map<UUID, Integer> LAST_OTHER_MESSAGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_REMINDER = new ConcurrentHashMap<>();

    private PropSwapReminder() {}

    /**
     * Notes any action bar message sent to the player, other than the reminder itself.
     */
    public static void packetSent(Player player, Packet<?> packet) {
        final var message = switch (packet) {
            case ClientboundSetActionBarTextPacket actionBar -> actionBar.text();
            case ClientboundSystemChatPacket chat when chat.overlay() -> chat.content();
            default -> null;
        };

        if (message != null && !MESSAGE.equals(message)) {
            LAST_OTHER_MESSAGE.put(player.getUUID(), player.level().getServer().getTickCount());
        }
    }

    /**
     * Whether the reminder should be (re)sent to the player right now.
     */
    public static boolean shouldRemind(Player player, int now) {
        if (!PropSwapMode.isEnabled(player) || !holdingTool(player)) {
            return false;
        }

        final var lastOther = LAST_OTHER_MESSAGE.get(player.getUUID());
        if (lastOther != null && now - lastOther < BACK_OFF_TICKS) {
            return false;
        }

        final var lastReminder = LAST_REMINDER.get(player.getUUID());
        return lastReminder == null || now - lastReminder >= REFRESH_TICKS;
    }

    public static void remind(MinecraftServer server) {
        final var now = server.getTickCount();
        for (final var player : server.getPlayerList().getPlayers()) {
            if (shouldRemind(player, now)) {
                LAST_REMINDER.put(player.getUUID(), now);
                player.sendOverlayMessage(MESSAGE);
            }
        }
    }

    /**
     * Clears the reminder from the player's action bar now, rather than leaving it to fade.
     */
    public static void hide(Player player) {
        player.sendOverlayMessage(Component.empty());
        forget(player);
    }

    public static void forget(Player player) {
        LAST_OTHER_MESSAGE.remove(player.getUUID());
        LAST_REMINDER.remove(player.getUUID());
    }

    public static void clear() {
        LAST_OTHER_MESSAGE.clear();
        LAST_REMINDER.clear();
    }

    private static boolean holdingTool(Player player) {
        return PropSwapTool.isTool(player.getItemInHand(InteractionHand.MAIN_HAND))
            || PropSwapTool.isTool(player.getItemInHand(InteractionHand.OFF_HAND));
    }
}
