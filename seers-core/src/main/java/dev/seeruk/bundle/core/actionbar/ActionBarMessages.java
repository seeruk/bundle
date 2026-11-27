package dev.seeruk.bundle.core.actionbar;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Messages kept in players' action bars for as long as they apply, e.g. "PropSwap enabled". Each
 * mod registers its message, and whichever apply to a player take turns. They're refreshed before
 * they fade, shown straight away when they start to apply or their text changes (unless backing
 * off), and cleared when none do.
 *
 * <p>They back off for a few seconds whenever anything else uses the action bar, so they never
 * hide other messages.
 */
public final class ActionBarMessages {
    // Action bar text fades after about 3 seconds, so each message is shown for a bit less
    static final int REFRESH_TICKS = 40;
    static final int BACK_OFF_TICKS = 80;

    // Mods register their messages on initialization, from there this is read-only (not enforced)
    private static final Map<Identifier, Message> MESSAGES = new LinkedHashMap<>();

    // Packets can be sent from other threads (e.g. by other mods)
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private ActionBarMessages() {}

    @FunctionalInterface
    public interface Message {
        /**
         * @param player The player who might see the message.
         * @return The message, if it applies to the player right now.
         */
        Optional<Component> message(ServerPlayer player);
    }

    /**
     * Registers a message, which must happen while mods initialize.
     * Messages take turns in the order they're registered.
     */
    public static void register(Identifier id, Message message) {
        MESSAGES.put(id, message);
    }

    /**
     * Shows each online player whichever message is due, if any.
     */
    public static void updateAll(MinecraftServer server) {
        if (MESSAGES.isEmpty()) {
            return;
        }

        for (final var player : server.getPlayerList().getPlayers()) {
            update(player, server.getTickCount());
        }
    }

    /**
     * Shows the player whichever message is due, if any, or clears the one showing if it no longer
     * applies.
     *
     * @return Whether anything was sent.
     */
    static boolean update(ServerPlayer player, int now) {
        final var state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        final var applying = applying(player);
        final var appeared = applying.keySet().stream()
            .filter(id -> !state.applying.contains(id))
            .findFirst();
        state.applying = Set.copyOf(applying.keySet());

        if (now - state.otherMessageAt < BACK_OFF_TICKS) {
            return false;
        }

        final var stillShowing = state.showing != null && applying.containsKey(state.showing);
        if (applying.isEmpty()) {
            if (state.showing == null) {
                return false;
            }

            send(player, state, null, Component.empty(), now);
            return true;
        }

        if (appeared.isEmpty() && stillShowing && now - state.shownAt < REFRESH_TICKS) {
            // Its text changed (e.g. a count in it), so it's updated, without starting a new turn
            final var text = applying.get(state.showing);
            if (text.equals(state.lastSent)) {
                return false;
            }

            send(player, state, state.showing, text, state.shownAt);
            return true;
        }

        final var next = appeared.orElseGet(() -> after(List.copyOf(applying.keySet()), state.showing));
        send(player, state, next, applying.get(next), now);
        return true;
    }

    /**
     * Notes any action bar message sent to the player that isn't one of these, so they back off.
     * Only players these have seen are noted, so a player who's left isn't remembered again.
     */
    public static void packetSent(ServerPlayer player, Packet<?> packet) {
        final var message = switch (packet) {
            case ClientboundBundlePacket bundle -> {
                bundle.subPackets().forEach(inside -> packetSent(player, inside));
                yield null;
            }
            case ClientboundSetActionBarTextPacket actionBar -> actionBar.text();
            case ClientboundSystemChatPacket chat when chat.overlay() -> chat.content();
            default -> null;
        };
        if (message == null) {
            return;
        }

        STATES.computeIfPresent(
            player.getUUID(), (id, state) -> {
                if (!message.equals(state.lastSent)) {
                    state.otherMessageAt = player.level().getServer().getTickCount();
                }
                return state;
            }
        );
    }

    /**
     * The message showing in the player's action bar, if one of these is.
     */
    public static Optional<Identifier> showing(ServerPlayer player) {
        return Optional.ofNullable(STATES.get(player.getUUID())).map(state -> state.showing);
    }

    public static void forget(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    public static void clear() {
        STATES.clear();
    }

    private static Map<Identifier, Component> applying(ServerPlayer player) {
        final var applying = new LinkedHashMap<Identifier, Component>();
        MESSAGES.forEach((id, message) -> message.message(player).ifPresent(text -> applying.put(id, text)));
        return applying;
    }

    /**
     * The message after the given one, going back to the first after the last, or the first if
     * none is showing (or the one showing no longer applies).
     */
    private static Identifier after(List<Identifier> ids, Identifier current) {
        // indexOf refuses null in an immutable list, rather than not finding it
        final var index = current == null ? -1 : ids.indexOf(current);
        return ids.get((index + 1) % ids.size());
    }

    private static void send(ServerPlayer player, State state, Identifier id, Component message, int shownAt) {
        // Set first, as sending passes the message straight back through packetSent
        state.lastSent = message;
        state.showing = id;
        state.shownAt = shownAt;
        player.sendOverlayMessage(message);
    }

    /**
     * What a player's been shown. Only the last message sent and when something else was sent are
     * touched by other threads.
     */
    private static final class State {
        private volatile Component lastSent;
        private volatile int otherMessageAt = Integer.MIN_VALUE / 2;
        private Set<Identifier> applying = Set.of();
        private Identifier showing;
        private int shownAt;
    }
}
