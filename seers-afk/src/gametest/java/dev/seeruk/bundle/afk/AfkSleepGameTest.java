package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.afk.api.AfkReason;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.players.SleepStatus;

import java.util.List;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;


public class AfkSleepGameTest {
    @GameTest
    public void afkPlayersArentCountedForSleeping(GameTestHelper helper) {
        final var awake = player(helper);
        final var afk = player(helper);
        AfkStatus.toggle(afk, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);

        final var sleepStatus = new SleepStatus();
        sleepStatus.update(List.of(awake, afk));

        helper.assertValueEqual(sleepStatus.sleepersNeeded(100), 1, "sleepers needed");
        helper.succeed();
    }

    @GameTest
    public void playersWhoArentAfkAreCounted(GameTestHelper helper) {
        final var sleepStatus = new SleepStatus();
        sleepStatus.update(List.of(player(helper), player(helper)));

        helper.assertValueEqual(sleepStatus.sleepersNeeded(100), 2, "sleepers needed");
        helper.succeed();
    }
}
