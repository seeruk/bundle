package dev.seeruk.bundle.nicks;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.nicks.api.NicksEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.nicks.NicksFixtures.player;
import static dev.seeruk.bundle.nicks.NicksFixtures.nameAndId;

public class NicknamesGameTest {
    private static final EventLog<Changed> EVENTS = new EventLog<>();

    static {
        NicksEvents.CHANGED.register((server, player, original, edited) ->
            EVENTS.record(new Changed(player.id(), text(original), text(edited))));
    }

    @GameTest
    public void setStoresNicknameAndFiresEvent(GameTestHelper helper) {
        final var player = player(helper);

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), "Seer", false);

        helper.assertValueEqual(text(Nicknames.get(player.getUUID())), Optional.of("Seer"), "nickname");
        helper.assertValueEqual(eventsFor(player.getUUID()), List.of(new Changed(player.getUUID(), Optional.empty(), Optional.of("Seer"))), "events");
        helper.succeed();
    }

    @GameTest
    public void displayNameShowsPrefixedNickname(GameTestHelper helper) {
        final var player = player(helper);

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), "Seer", false);

        helper.assertValueEqual(player.getDisplayName().getString(), Nickname.PREFIX + "Seer", "display name");
        helper.succeed();
    }

    @GameTest
    public void playersCanOnlyUseColours(GameTestHelper helper) {
        final var player = player(helper);

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), "<b>Seer</b>", false);

        helper.assertValueEqual(text(Nicknames.get(player.getUUID())), Optional.of("<b>Seer</b>"), "nickname");
        helper.succeed();
    }

    @GameTest
    public void adminsCanUseAnyFormatting(GameTestHelper helper) {
        final var player = player(helper);

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), "<b>Seer</b>", true);

        final var nickname = Nicknames.get(player.getUUID()).orElseThrow();
        helper.assertValueEqual(nickname.getString(), "Seer", "nickname");
        helper.assertTrue(nickname.toFlatList().getFirst().getStyle().isBold(), "Nickname wasn't bold");
        helper.succeed();
    }

    @GameTest
    public void unchangedNicknameSucceedsWithoutFiringEvent(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        Nicknames.set(server, nameAndId(player), "Seer", false);
        final var mark = EVENTS.mark();

        final var result = Nicknames.set(server, nameAndId(player), "Seer", false);

        helper.assertTrue(result instanceof NickEditResult.Edited, "Edit didn't succeed");
        helper.assertTrue(EVENTS.matchingSince(mark, event -> event.id().equals(player.getUUID())).isEmpty(), "Event fired for an unchanged nickname");
        helper.succeed();
    }

    @GameTest
    public void resetRemovesNicknameAndFiresEvent(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        Nicknames.set(server, nameAndId(player), "Seer", false);

        Nicknames.reset(server, nameAndId(player));

        helper.assertValueEqual(Nicknames.get(player.getUUID()), Optional.empty(), "nickname");
        helper.assertValueEqual(player.getDisplayName().getString(), player.getGameProfile().name(), "display name");
        helper.assertValueEqual(eventsFor(player.getUUID()).getLast(), new Changed(player.getUUID(), Optional.of("Seer"), Optional.empty()), "event");
        helper.succeed();
    }

    @GameTest
    public void resetFailsWithoutNickname(GameTestHelper helper) {
        final var player = player(helper);

        final var result = Nicknames.reset(helper.getLevel().getServer(), nameAndId(player));

        helper.assertTrue(result instanceof NickEditResult.NoNickname, "Edit didn't fail with NoNickname");
        helper.assertTrue(eventsFor(player.getUUID()).isEmpty(), "Event fired for a failed edit");
        helper.succeed();
    }

    @GameTest
    public void ownUsernameIsAllowed(GameTestHelper helper) {
        // Mock players' usernames are too long to be nicknames, so this uses an offline player
        final var player = new NameAndId(UUID.randomUUID(), "Seer");

        final var result = Nicknames.set(helper.getLevel().getServer(), player, "seer", false);

        helper.assertTrue(result instanceof NickEditResult.Edited, "Edit didn't succeed");
        helper.succeed();
    }

    @GameTest
    public void offlinePlayersCanBeGivenNicknames(GameTestHelper helper) {
        final var offline = new NameAndId(UUID.randomUUID(), "OfflinePlayer");

        Nicknames.set(helper.getLevel().getServer(), offline, "<red>Seer</red>", true);

        helper.assertValueEqual(text(Nicknames.get(offline.id())), Optional.of("Seer"), "nickname");
        helper.succeed();
    }

    // Formatting is covered by the text parser's own tests, so these compare plain text
    private record Changed(UUID id, Optional<String> original, Optional<String> edited) {}

    private static List<Changed> eventsFor(UUID id) {
        return EVENTS.matching(event -> event.id().equals(id));
    }

    private static Optional<String> text(Optional<Component> component) {
        return component.map(Component::getString);
    }
}
