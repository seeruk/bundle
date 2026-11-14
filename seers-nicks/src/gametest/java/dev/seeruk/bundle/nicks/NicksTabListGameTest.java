package dev.seeruk.bundle.nicks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import static dev.seeruk.bundle.nicks.NicksFixtures.nameAndId;
import static dev.seeruk.bundle.nicks.NicksFixtures.nickname;
import static dev.seeruk.bundle.nicks.NicksFixtures.player;

public class NicksTabListGameTest {
    @GameTest
    public void showsNicknameAndRealName(GameTestHelper helper) {
        final var player = player(helper);
        final var nickname = nickname("Seer");

        Nicknames.set(helper.getLevel().getServer(), nameAndId(player), nickname, false);

        final var expected = Nickname.PREFIX + nickname + " (" + player.getGameProfile().name() + ")";
        helper.assertValueEqual(player.getTabListDisplayName().getString(), expected, "tab list name");
        helper.succeed();
    }

    @GameTest
    public void leavesPlayersWithoutNicknamesToVanilla(GameTestHelper helper) {
        final var player = player(helper);

        helper.assertTrue(player.getTabListDisplayName() == null, "Player without a nickname had a tab list name");
        helper.succeed();
    }

    @GameTest
    public void includesTeamStyling(GameTestHelper helper) {
        final var scoreboard = helper.getLevel().getServer().getScoreboard();
        final var player = player(helper);
        final var team = scoreboard.addPlayerTeam("nicks_" + player.getUUID().toString().substring(0, 8));
        final var nickname = nickname("Seer");

        try {
            Nicknames.set(helper.getLevel().getServer(), nameAndId(player), nickname, false);
            team.setPlayerPrefix(Component.literal("[T] "));
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);

            helper.assertTrue(
                player.getTabListDisplayName().getString().startsWith("[T] " + Nickname.PREFIX + nickname),
                "Tab list name didn't include the team's prefix: " + player.getTabListDisplayName().getString()
            );
        } finally {
            scoreboard.removePlayerTeam(team);
        }

        helper.succeed();
    }
}
