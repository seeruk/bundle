package dev.seeruk.bundle.core.actionbar;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class ActionBarMessagesGameTest {
    private static final Identifier FIRST = Identifier.fromNamespaceAndPath("seers-core-gametest", "first");
    private static final Identifier SECOND = Identifier.fromNamespaceAndPath("seers-core-gametest", "second");

    // Which players each message applies to, so each test only sees the messages it turns on
    private static final Set<UUID> FIRST_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SECOND_PLAYERS = ConcurrentHashMap.newKeySet();

    static {
        ActionBarMessages.register(FIRST, player -> applies(FIRST_PLAYERS, player, "First"));
        ActionBarMessages.register(SECOND, player -> applies(SECOND_PLAYERS, player, "Second"));
    }

    @GameTest
    public void showsAMessageToRegisteredPlayers(GameTestHelper helper) {
        final var player = player(helper);
        FIRST_PLAYERS.add(player.getUUID());

        helper.assertTrue(ActionBarMessages.update(player, now(helper)), "Nothing was sent");
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(FIRST), "showing");
        helper.succeed();
    }

    @GameTest
    public void refreshesMessageBeforeItFades(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());
        ActionBarMessages.update(player, now);

        helper.assertFalse(ActionBarMessages.update(player, now + 1), "Sent again straight away");
        helper.assertTrue(
            ActionBarMessages.update(player, now + ActionBarMessages.REFRESH_TICKS),
            "Never refreshed"
        );
        helper.succeed();
    }

    @GameTest
    public void messagesTakeTurns(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());
        SECOND_PLAYERS.add(player.getUUID());

        ActionBarMessages.update(player, now);
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(FIRST), "first turn");

        ActionBarMessages.update(player, now + ActionBarMessages.REFRESH_TICKS);
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(SECOND), "second turn");

        ActionBarMessages.update(player, now + ActionBarMessages.REFRESH_TICKS * 2);
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(FIRST), "third turn");
        helper.succeed();
    }

    @GameTest
    public void showsANewMessageStraightAway(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());
        ActionBarMessages.update(player, now);

        SECOND_PLAYERS.add(player.getUUID());

        helper.assertTrue(ActionBarMessages.update(player, now + 1), "Waited to show the new message");
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(SECOND), "showing");
        helper.succeed();
    }

    @GameTest
    public void switchesWhenTheMessageShowingStopsApplying(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());
        SECOND_PLAYERS.add(player.getUUID());
        ActionBarMessages.update(player, now);

        FIRST_PLAYERS.remove(player.getUUID());

        helper.assertTrue(ActionBarMessages.update(player, now + 1), "Kept showing a message that stopped applying");
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(SECOND), "showing");
        helper.succeed();
    }

    @GameTest
    public void clearsWhenNothingApplies(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());
        ActionBarMessages.update(player, now);

        FIRST_PLAYERS.remove(player.getUUID());

        helper.assertTrue(ActionBarMessages.update(player, now + 1), "Didn't clear the message");
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.empty(), "showing");
        helper.assertFalse(ActionBarMessages.update(player, now + 2), "Cleared it again");
        helper.succeed();
    }

    @GameTest
    public void backsOffForOtherActionBarMessages(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());

        // Check that normal chat isn't backed off for, since it uses the same packet
        ActionBarMessages.packetSent(player, new ClientboundSystemChatPacket(Component.literal("hi"), false));
        helper.assertTrue(ActionBarMessages.update(player, now), "Backed off for chat");

        ActionBarMessages.packetSent(player, new ClientboundSystemChatPacket(Component.literal("hi"), true));
        helper.assertFalse(
            ActionBarMessages.update(player, now + ActionBarMessages.REFRESH_TICKS),
            "Didn't back off"
        );
        helper.assertTrue(
            ActionBarMessages.update(player, now + ActionBarMessages.BACK_OFF_TICKS),
            "Never came back"
        );
        helper.succeed();
    }

    @GameTest
    public void showsAMessageThatStartedToApplyWhileBackingOff(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        track(player, now);
        ActionBarMessages.packetSent(player, new ClientboundSystemChatPacket(Component.literal("hi"), true));

        FIRST_PLAYERS.add(player.getUUID());
        ActionBarMessages.update(player, now);

        helper.assertTrue(
            ActionBarMessages.update(player, now + ActionBarMessages.BACK_OFF_TICKS),
            "Never showed the message"
        );
        helper.assertValueEqual(ActionBarMessages.showing(player), Optional.of(FIRST), "showing");
        helper.succeed();
    }

    @GameTest
    public void backsOffForTitleActionBarMessages(GameTestHelper helper) {
        // `/title <player> actionbar ...` uses its own packet, rather than an overlay chat message
        final var player = player(helper);
        final var now = now(helper);
        track(player, now);
        FIRST_PLAYERS.add(player.getUUID());

        ActionBarMessages.packetSent(player, new ClientboundSetActionBarTextPacket(Component.literal("hi")));

        helper.assertFalse(ActionBarMessages.update(player, now), "Didn't back off");
        helper.succeed();
    }

    @GameTest
    public void backsOffForMessagesInBundlesOfPackets(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        track(player, now);
        FIRST_PLAYERS.add(player.getUUID());

        ActionBarMessages.packetSent(player, new ClientboundBundlePacket(List.of(
            new ClientboundSetActionBarTextPacket(Component.literal("hi"))
        )));

        helper.assertFalse(ActionBarMessages.update(player, now), "Didn't back off");
        helper.succeed();
    }

    @GameTest
    public void forgetsPlayersForGood(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        track(player, now);
        ActionBarMessages.forget(player);

        // e.g. a message sent after they've left, which mustn't bring them back
        ActionBarMessages.packetSent(player, new ClientboundSystemChatPacket(Component.literal("hi"), true));
        FIRST_PLAYERS.add(player.getUUID());

        helper.assertTrue(ActionBarMessages.update(player, now), "Remembered a forgotten player's back-off");
        helper.succeed();
    }

    @GameTest
    public void doesNotBackOffForItsOwnMessages(GameTestHelper helper) {
        final var player = player(helper);
        final var now = now(helper);
        FIRST_PLAYERS.add(player.getUUID());

        // Sent for real, so it passes through the packet mixin
        ActionBarMessages.update(player, now);
        // Other mods might rebuild overlay messages before they're sent
        ActionBarMessages.packetSent(player, new ClientboundSystemChatPacket(message("First"), true));

        helper.assertTrue(
            ActionBarMessages.update(player, now + ActionBarMessages.REFRESH_TICKS),
            "Backed off for itself"
        );
        helper.succeed();
    }

    /**
     * Starts tracking the player, as core's tick would for a real one, so back-offs sent to them
     * are noted. Nothing applies to them yet, so nothing is shown to them.
     */
    private static void track(ServerPlayer player, int now) {
        ActionBarMessages.update(player, now);
    }

    private static Optional<Component> applies(Set<UUID> players, ServerPlayer player, String text) {
        return players.contains(player.getUUID()) ? Optional.of(message(text)) : Optional.empty();
    }

    private static Component message(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static int now(GameTestHelper helper) {
        return helper.getLevel().getServer().getTickCount();
    }
}
