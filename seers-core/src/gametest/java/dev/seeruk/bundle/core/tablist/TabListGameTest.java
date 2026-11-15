package dev.seeruk.bundle.core.tablist;

import dev.seeruk.bundle.core.testing.TestPlayers;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TabListGameTest {
    // Listeners only change tagged players, so they don't affect other tests
    private static final String REPLACED = "core_tab_list_replaced";
    private static final String DECORATED = "core_tab_list_decorated";

    static {
        // Registered before the replacing listener, to check the phases decide the order
        TabList.NAME.register((player, name) ->
            player.entityTags().contains(DECORATED) ? Component.empty().append(name).append(" [Decorated]") : name);
        TabList.NAME.register(TabList.REPLACE, (player, name) ->
            player.entityTags().contains(REPLACED) ? Component.literal("Replaced") : name);
        TabList.ORDER.register((player, order) -> player.entityTags().contains(DECORATED) ? order - 1 : order);
    }

    @GameTest
    public void leavesUnchangedPlayersToVanilla(GameTestHelper helper) {
        final var player = player(helper);

        helper.assertTrue(player.getTabListDisplayName() == null, "Unchanged player had a tab list name");
        helper.assertValueEqual(player.getTabListOrder(), 0, "tab list order");
        helper.succeed();
    }

    @GameTest
    public void changesStartFromTheTeamStyledName(GameTestHelper helper) {
        final var scoreboard = helper.getLevel().getServer().getScoreboard();
        final var player = player(helper, DECORATED);
        final var team = scoreboard.addPlayerTeam("core_" + player.getUUID().toString().substring(0, 8));

        try {
            team.setPlayerPrefix(Component.literal("[T] "));
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);

            final var expected = "[T] " + player.getGameProfile().name() + " [Decorated]";
            helper.assertValueEqual(player.getTabListDisplayName().getString(), expected, "tab list name");
            helper.assertValueEqual(player.getTabListOrder(), -1, "tab list order");
        } finally {
            scoreboard.removePlayerTeam(team);
        }

        helper.succeed();
    }

    @GameTest
    public void replacingListenersRunFirst(GameTestHelper helper) {
        final var player = player(helper, REPLACED, DECORATED);

        helper.assertValueEqual(player.getTabListDisplayName().getString(), "Replaced [Decorated]", "tab list name");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, String... tags) {
        final var player = TestPlayers.player(helper);
        for (final var tag : tags) {
            player.addTag(tag);
        }
        return player;
    }
}
