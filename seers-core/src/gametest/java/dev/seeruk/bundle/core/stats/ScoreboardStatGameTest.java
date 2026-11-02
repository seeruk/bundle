package dev.seeruk.bundle.core.stats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * The scoreboard is shared by every test on the server, so each test uses its own objective and
 * removes it again afterwards.
 */
public class ScoreboardStatGameTest {
    private static final String MOD_ID = "seers-core-gametest";

    @GameTest
    public void ensureObjectiveAddsMissingObjective(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var stat = newStat(server, "test_stat", "Test Stat");

        try {
            stat.ensureObjective(server);

            final var objective = server.getScoreboard().getObjective(stat.objectiveId());

            helper.assertTrue(objective != null, "Objective wasn't added as " + stat.objectiveId());
            helper.assertValueEqual(ObjectiveCriteria.DUMMY, objective.getCriteria(), "criteria");
            helper.assertValueEqual("Test Stat", objective.getDisplayName().getString(), "display name");
        } finally {
            removeObjective(server, stat);
        }

        helper.succeed();
    }

    @GameTest
    public void ensureObjectiveDoesNotResetObjective(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var stat = newStat(server, "test_stat", "Test Stat");
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);

        try {
            stat.ensureObjective(server);
            stat.add(player, 5);
            stat.ensureObjective(server);

            helper.assertValueEqual(5, score(server, stat, player), "score after re-ensuring");
        } finally {
            removeObjective(server, stat);
        }

        helper.succeed();
    }

    @GameTest
    public void addIncrementsScoreByGivenAmount(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var stat = newStat(server, "test_stat", "Test Stat");
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);

        try {
            stat.ensureObjective(server);

            stat.add(player, 42);
            helper.assertValueEqual(42, score(server, stat, player), "score after first add");

            stat.add(player, 27);
            helper.assertValueEqual(69, score(server, stat, player), "score after second add");
        } finally {
            removeObjective(server, stat);
        }

        helper.succeed();
    }

    @GameTest
    public void addDoesNothingWithoutObjective(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var stat = newStat(server, "test_stat", "Test Stat");
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);

        stat.add(player, 1);

        helper.assertTrue(objective(server, stat) == null, "Objective was added by add");
        helper.succeed();
    }

    /**
     * The objective's name has to be usable in commands (e.g. a `:` in it can't be parsed), or
     * players can't display or query it.
     */
    @GameTest
    public void objectiveCanBeUsedInCommands(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var stat = newStat(server, "test_stat", "Test Stat");
        final var dispatcher = server.getCommands().getDispatcher();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            stat.ensureObjective(server);
            dispatcher.execute("scoreboard objectives setdisplay sidebar " + stat.objectiveId(), source);
            dispatcher.execute("scoreboard objectives setdisplay sidebar", source);
        } catch (CommandSyntaxException e) {
            helper.fail("Objective couldn't be used in a command: " + e.getMessage());
        } finally {
            removeObjective(server, stat);
        }

        helper.succeed();
    }

    /**
     * Creates a brand-new stat, removing any existing objective with the same name first.
     */
    private static ScoreboardStat newStat(MinecraftServer server, String name, String displayName) {
        final var stat = new ScoreboardStat(MOD_ID, name, displayName);
        removeObjective(server, stat);
        return stat;
    }

    private static Objective objective(MinecraftServer server, ScoreboardStat stat) {
        return server.getScoreboard().getObjective(stat.objectiveId());
    }

    private static void removeObjective(MinecraftServer server, ScoreboardStat stat) {
        final var objective = objective(server, stat);
        if (objective != null) {
            server.getScoreboard().removeObjective(objective);
        }
    }

    private static int score(MinecraftServer server, ScoreboardStat stat, Player player) {
        final var info = server.getScoreboard().getPlayerScoreInfo(player, objective(server, stat));
        return info == null ? 0 : info.value();
    }
}
