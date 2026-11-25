package dev.seeruk.bundle.core.names;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class PlayerNamesGameTest {
    // The listener only names these players, so it doesn't affect other tests
    private static final Map<UUID, Component> NAMES = new ConcurrentHashMap<>();

    static {
        PlayerNames.NAME.register((server, id) -> Optional.ofNullable(NAMES.get(id)));
    }

    private static NameAndId offline() {
        final var id = UUID.randomUUID();
        return new NameAndId(id, "offline_" + id.toString().substring(0, 8));
    }

    @GameTest
    public void displayNameIsUsernameForOnlinePlayers(GameTestHelper helper) {
        final var player = player(helper);

        final var name = PlayerNames.displayName(helper.getLevel().getServer(), player.getUUID());
        helper.assertValueEqual(name.getString(), player.getGameProfile().name(), "display name");
        helper.succeed();
    }

    @GameTest
    public void displayNameIsLastSeenUsernameForOfflinePlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = offline();
        server.services().nameToIdCache().add(player);

        helper.assertValueEqual(PlayerNames.displayName(server, player.id()).getString(), player.name(), "display name");
        helper.succeed();
    }

    @GameTest
    public void displayNameIsUuidForUnknownPlayers(GameTestHelper helper) {
        final var id = UUID.randomUUID();

        helper.assertValueEqual(PlayerNames.displayName(helper.getLevel().getServer(), id).getString(), id.toString(), "display name");
        helper.succeed();
    }

    @GameTest
    public void displayNameUsesNameFromListeners(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = offline();
        server.services().nameToIdCache().add(player);
        NAMES.put(player.id(), Component.literal("Renamed"));

        helper.assertValueEqual(PlayerNames.displayName(server, player.id()).getString(), "Renamed", "display name");
        helper.succeed();
    }

    @GameTest
    public void displayNameIsStyledForOfflinePlayersTeams(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var scoreboard = server.getScoreboard();
        final var player = offline();
        final var team = scoreboard.addPlayerTeam("core_" + player.id().toString().substring(0, 8));
        server.services().nameToIdCache().add(player);
        NAMES.put(player.id(), Component.literal("Renamed"));

        try {
            team.setPlayerPrefix(Component.literal("[T] "));
            scoreboard.addPlayerToTeam(player.name(), team);

            helper.assertValueEqual(PlayerNames.displayName(server, player.id()).getString(), "[T] Renamed", "display name");
        } finally {
            scoreboard.removePlayerTeam(team);
        }

        helper.succeed();
    }

    @GameTest
    public void displayNameDoesNothingWhenHoveredOrClicked(GameTestHelper helper) {
        final var player = player(helper);
        final var style = PlayerNames.displayName(helper.getLevel().getServer(), player.getUUID()).getStyle();

        helper.assertTrue(style.getHoverEvent() == null, "Display name has a hover event");
        helper.assertTrue(style.getClickEvent() == null, "Display name has a click event");
        helper.succeed();
    }
}
