package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkReason;
import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;


public class AfkTabListGameTest {
    @GameTest
    public void afkPlayersAreMarkedAndMovedDown(GameTestHelper helper) {
        final var player = player(helper);

        AfkStatus.toggle(player, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);

        helper.assertValueEqual(
            player.getTabListDisplayName().getString(),
            player.getGameProfile().name() + " [AFK]",
            "tab list name"
        );
        helper.assertValueEqual(player.getTabListOrder(), -1, "tab list order");
        helper.succeed();
    }

    @GameTest
    public void playersWhoAreBackAreLeftAlone(GameTestHelper helper) {
        final var player = player(helper);

        AfkStatus.toggle(player, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);
        AfkStatus.toggle(player, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);

        helper.assertTrue(player.getTabListDisplayName() == null, "Player who's back had a tab list name");
        helper.assertValueEqual(player.getTabListOrder(), 0, "tab list order");
        helper.succeed();
    }

    @GameTest
    public void templateWrapsTheNameSoFar(GameTestHelper helper) {
        final var player = player(helper);
        AfkStatus.toggle(player, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);

        final var name = AfkTabList.name(
            player,
            Component.literal("Seer"),
            TextTemplate.parse("[AFK] ${tab_list_name}")
        );

        helper.assertValueEqual(name.getString(), "[AFK] Seer", "tab list name");
        helper.succeed();
    }
}
