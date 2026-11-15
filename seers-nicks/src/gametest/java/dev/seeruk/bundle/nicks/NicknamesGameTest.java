package dev.seeruk.bundle.nicks;

import dev.seeruk.bundle.core.testing.EventLog;
import dev.seeruk.bundle.nicks.api.NicksEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.nicks.NicksFixtures.nameAndId;
import static dev.seeruk.bundle.nicks.NicksFixtures.nickname;
import static dev.seeruk.bundle.nicks.NicksFixtures.offline;

public class NicknamesGameTest {
    private static final EventLog<Changed> EVENTS = new EventLog<>();

    static {
        NicksEvents.CHANGED.register((server, player, original, edited) ->
            EVENTS.record(new Changed(player.id(), text(original), text(edited))));
    }

    @GameTest
    public void setStoresNicknameAndFiresEvent(GameTestHelper helper) {
        final var player = player(helper);
        final var nickname = nickname("Seer");

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), nickname, false);

        helper.assertValueEqual(text(Nicknames.get(player.getUUID())), Optional.of(nickname), "nickname");
        helper.assertValueEqual(eventsFor(player.getUUID()), List.of(new Changed(player.getUUID(), Optional.empty(), Optional.of(nickname))), "events");
        helper.succeed();
    }

    @GameTest
    public void displayNameShowsPrefixedNickname(GameTestHelper helper) {
        final var player = player(helper);
        final var nickname = nickname("Seer");

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), nickname, false);

        helper.assertValueEqual(player.getDisplayName().getString(), Nickname.PREFIX + nickname, "display name");
        helper.succeed();
    }

    @GameTest
    public void playersCanOnlyUseColours(GameTestHelper helper) {
        final var player = player(helper);
        final var nickname = "<b>" + nickname("Seer") + "</b>";

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), nickname, false);

        helper.assertValueEqual(text(Nicknames.get(player.getUUID())), Optional.of(nickname), "nickname");
        helper.succeed();
    }

    @GameTest
    public void adminsCanUseAnyFormatting(GameTestHelper helper) {
        final var player = player(helper);
        final var name = nickname("Seer");

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), "<b>" + name + "</b>", true);

        final var nickname = Nicknames.get(player.getUUID()).orElseThrow();
        helper.assertValueEqual(nickname.getString(), name, "nickname");
        helper.assertTrue(nickname.toFlatList().getFirst().getStyle().isBold(), "Nickname wasn't bold");
        helper.succeed();
    }

    @GameTest
    public void unchangedNicknameSucceedsWithoutFiringEvent(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var nickname = nickname("Seer");
        Nicknames.set(server, nameAndId(player), nickname, false);
        final var mark = EVENTS.mark();

        final var result = Nicknames.set(server, nameAndId(player), nickname, false);

        helper.assertTrue(result instanceof NickEditResult.Edited, "Edit didn't succeed");
        helper.assertTrue(EVENTS.matchingSince(mark, event -> event.id().equals(player.getUUID())).isEmpty(), "Event fired for an unchanged nickname");
        helper.succeed();
    }

    @GameTest
    public void resetRemovesNicknameAndFiresEvent(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var nickname = nickname("Seer");
        Nicknames.set(server, nameAndId(player), nickname, false);

        Nicknames.reset(server, nameAndId(player));

        helper.assertValueEqual(Nicknames.get(player.getUUID()), Optional.empty(), "nickname");
        helper.assertValueEqual(player.getDisplayName().getString(), player.getGameProfile().name(), "display name");
        helper.assertValueEqual(eventsFor(player.getUUID()).getLast(), new Changed(player.getUUID(), Optional.of(nickname), Optional.empty()), "event");
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
        final var player = nameAndId(player(helper));

        final var result = Nicknames.set(helper.getLevel().getServer(), player, player.name().toUpperCase(), false);

        helper.assertTrue(result instanceof NickEditResult.Edited, "Edit didn't succeed");
        helper.succeed();
    }

    @GameTest
    public void offlinePlayersCanBeGivenNicknames(GameTestHelper helper) {
        final var player = offline("Offline");
        final var nickname = nickname("Seer");

        Nicknames.set(helper.getLevel().getServer(), player, "<red>" + nickname + "</red>", true);

        helper.assertValueEqual(text(Nicknames.get(player.id())), Optional.of(nickname), "nickname");
        helper.succeed();
    }

    @GameTest
    public void anotherPlayersNicknameIsRefused(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var nicked = offline("Nicked");
        final var player = offline("Player");
        final var nickname = nickname("Seer");
        Nicknames.set(server, nicked, "<red>" + nickname + "</red>", false);

        final var result = Nicknames.set(server, player, nickname.toUpperCase(), false);

        helper.assertTrue(result instanceof NickEditResult.NicknameTaken, "Edit didn't fail with NicknameTaken");
        helper.assertValueEqual(Nicknames.get(player.id()), Optional.empty(), "nickname");
        helper.succeed();
    }

    @GameTest
    public void joiningPlayerReleasesTheirUsername(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var joining = offline("Joining");
        final var player = offline("Player");
        final var admin = offline("Admin");
        Nicknames.set(server, player, "<red>" + joining.name() + "</red>", false);
        Nicknames.set(server, admin, joining.name(), true);

        player(helper, joining);

        helper.assertValueEqual(Nicknames.get(player.id()), Optional.empty(), "player's nickname");
        helper.assertValueEqual(text(Nicknames.get(admin.id())), Optional.of(joining.name()), "admin-set nickname");
        helper.succeed();
    }

    @GameTest
    public void joiningPlayerKeepsTheirOwnUsernameAsNickname(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var joining = offline("Joining");
        Nicknames.set(server, joining, joining.name(), false);

        player(helper, joining);

        helper.assertValueEqual(text(Nicknames.get(joining.id())), Optional.of(joining.name()), "nickname");
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
