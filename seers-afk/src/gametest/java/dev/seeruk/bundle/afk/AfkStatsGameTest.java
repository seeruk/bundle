package dev.seeruk.bundle.afk;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.afk.api.AfkReason;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class AfkStatsGameTest {
    private static final String OBJECTIVE_ID = AfkStats.AFK_TICKS.objectiveId();

    @GameTest
    public void objectiveExistsAfterServerStarts(GameTestHelper helper) {
        final var objective = helper.getLevel().getServer().getScoreboard().getObjective(OBJECTIVE_ID);

        helper.assertTrue(objective != null, "Objective " + OBJECTIVE_ID + " doesn't exist");
        helper.succeed();
    }

    @GameTest
    public void objectiveCanBeUsedInCommands(GameTestHelper helper) {
        // Malformed objective names can stop objective-related commands from working, even though
        // the objective itself exists
        final var server = helper.getLevel().getServer();
        final var dispatcher = server.getCommands().getDispatcher();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            dispatcher.execute("scoreboard objectives setdisplay sidebar " + OBJECTIVE_ID, source);
            dispatcher.execute("scoreboard objectives setdisplay sidebar", source);
        } catch (CommandSyntaxException e) {
            helper.fail("Objective couldn't be used in a command: " + e.getMessage());
        }

        helper.succeed();
    }

    @GameTest
    public void afkPlayersGainOnePerTick(GameTestHelper helper) {
        final var afk = player(helper);
        final var back = player(helper);
        AfkStatus.toggle(afk, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);
        final var afkBefore = score(afk, AfkStats.AFK_TICKS);
        final var backBefore = score(back, AfkStats.AFK_TICKS);

        AfkStats.recordAfkTick(helper.getLevel().getServer());

        helper.assertValueEqual(score(afk, AfkStats.AFK_TICKS) - afkBefore, 1, "AFK player's score increase");
        helper.assertValueEqual(score(back, AfkStats.AFK_TICKS) - backBefore, 0, "other player's score increase");
        helper.succeed();
    }

    @GameTest
    public void afkPlayersGainAsTheServerTicks(GameTestHelper helper) {
        final var player = player(helper);
        AfkStatus.toggle(player, helper.getLevel().getServer().getTickCount(), AfkReason.COMMAND);
        final var before = score(player, AfkStats.AFK_TICKS);

        helper.runAfterDelay(
            5, () -> {
                helper.assertTrue(
                    score(player, AfkStats.AFK_TICKS) > before,
                    "Score didn't increase as the server ticked"
                );
                helper.succeed();
            }
        );
    }
}
