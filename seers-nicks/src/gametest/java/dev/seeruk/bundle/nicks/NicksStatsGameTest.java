package dev.seeruk.bundle.nicks;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.nicks.NicksFixtures.player;
import static dev.seeruk.bundle.nicks.NicksFixtures.nameAndId;
import static dev.seeruk.bundle.nicks.NicksFixtures.nickname;

public class NicksStatsGameTest {
    private static final String OBJECTIVE_ID = NicksStats.NICKNAMES_SET.objectiveId();

    @GameTest
    public void objectiveExistsAfterServerStarts(GameTestHelper helper) {
        final var objective = helper.getLevel().getServer().getScoreboard().getObjective(OBJECTIVE_ID);

        helper.assertTrue(objective != null, "Objective " + OBJECTIVE_ID + " doesn't exist");
        helper.succeed();
    }

    @GameTest
    public void objectiveCanBeUsedInCommands(GameTestHelper helper) {
        // This might not be obvious. Malformed objective names can cause objective-related commands
        // to fail, while the objective itself can still exist.
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
    public void settingNicknameAddsOne(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var before = score(player, NicksStats.NICKNAMES_SET);

        Nicknames.set(server, nameAndId(player), nickname("Seer"), false);
        Nicknames.set(server, nameAndId(player), nickname("Seer"), false);

        helper.assertValueEqual(score(player, NicksStats.NICKNAMES_SET) - before, 2, "score increase");
        helper.succeed();
    }

    @GameTest
    public void unchangedOrResetNicknameDoesNotAddToScore(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var nickname = nickname("Seer");
        Nicknames.set(server, nameAndId(player), nickname, false);
        final var before = score(player, NicksStats.NICKNAMES_SET);

        Nicknames.set(server, nameAndId(player), nickname, false);
        Nicknames.reset(server, nameAndId(player));

        helper.assertValueEqual(score(player, NicksStats.NICKNAMES_SET) - before, 0, "score increase");
        helper.succeed();
    }
}
