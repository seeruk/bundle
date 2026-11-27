package dev.seeruk.bundle.nicks;

import dev.seeruk.bundle.core.names.PlayerNames;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.seeruk.bundle.nicks.NicksFixtures.nickname;
import static dev.seeruk.bundle.nicks.NicksFixtures.offline;

public class NicksPlayerNamesGameTest {
    @GameTest
    public void showsNicknameForOfflinePlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = offline("Offline");
        final var nickname = nickname("Seer");
        server.services().nameToIdCache().add(player);

        Nicknames.set(server, player, nickname, false);

        helper.assertValueEqual(
            PlayerNames.displayName(server, player.id()).getString(),
            Nickname.PREFIX + nickname,
            "display name"
        );
        helper.succeed();
    }

    @GameTest
    public void leavesPlayersWithoutNicknamesToTheirUsername(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = offline("Offline");
        server.services().nameToIdCache().add(player);

        helper.assertValueEqual(
            PlayerNames.displayName(server, player.id()).getString(),
            player.name(),
            "display name"
        );
        helper.succeed();
    }
}
