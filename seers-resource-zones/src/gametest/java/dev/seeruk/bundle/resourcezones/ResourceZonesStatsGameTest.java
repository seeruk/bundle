package dev.seeruk.bundle.resourcezones;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.addZoneChunk;
import static dev.seeruk.bundle.resourcezones.ResourceZonesFixtures.moveTo;

// Zones are shared by the whole test server, so each test uses its own chunks
public class ResourceZonesStatsGameTest {
    private static final List<ScoreboardStat> STATS = List.of(
        ResourceZonesStats.ZONE_TICKS,
        ResourceZonesStats.ZONE_ENTRIES,
        ResourceZonesStats.BLOCKS_MINED
    );

    @GameTest
    public void objectivesExistAfterServerStarts(GameTestHelper helper) {
        final var scoreboard = helper.getLevel().getServer().getScoreboard();

        for (final var stat : STATS) {
            helper.assertTrue(
                scoreboard.getObjective(stat.objectiveId()) != null,
                "Objective " + stat.objectiveId() + " doesn't exist"
            );
        }
        helper.succeed();
    }

    @GameTest
    public void objectivesCanBeUsedInCommands(GameTestHelper helper) {
        // Malformed objective names can stop objective-related commands from working, even though
        // the objective itself exists
        final var server = helper.getLevel().getServer();
        final var dispatcher = server.getCommands().getDispatcher();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            for (final var stat : STATS) {
                dispatcher.execute("scoreboard objectives setdisplay sidebar " + stat.objectiveId(), source);
            }
            dispatcher.execute("scoreboard objectives setdisplay sidebar", source);
        } catch (CommandSyntaxException e) {
            helper.fail("Objective couldn't be used in a command: " + e.getMessage());
        }

        helper.succeed();
    }

    @GameTest
    public void playersInZonesGainOneTickPerTick(GameTestHelper helper) {
        final var inside = player(helper);
        final var outside = player(helper);
        moveTo(inside, addZoneChunk(helper, inside, new ChunkPos(6000, 0)));
        moveTo(outside, new ChunkPos(6001, 0));
        ZonePresence.update(inside);
        ZonePresence.update(outside);
        final var insideBefore = score(inside, ResourceZonesStats.ZONE_TICKS);
        final var outsideBefore = score(outside, ResourceZonesStats.ZONE_TICKS);

        ResourceZonesStats.recordZoneTick(helper.getLevel().getServer());

        helper.assertValueEqual(
            score(inside, ResourceZonesStats.ZONE_TICKS) - insideBefore,
            1,
            "inside player's score increase"
        );
        helper.assertValueEqual(
            score(outside, ResourceZonesStats.ZONE_TICKS) - outsideBefore,
            0,
            "outside player's score increase"
        );
        helper.succeed();
    }

    @GameTest
    public void enteringAZoneCountsAsAnEntry(GameTestHelper helper) {
        final var player = player(helper);
        moveTo(player, new ChunkPos(6010, 0));
        ZonePresence.update(player);
        final var before = score(player, ResourceZonesStats.ZONE_ENTRIES);

        moveTo(player, addZoneChunk(helper, player, new ChunkPos(6011, 0)));
        ZonePresence.update(player);

        helper.assertValueEqual(score(player, ResourceZonesStats.ZONE_ENTRIES) - before, 1, "score increase");
        helper.succeed();
    }

    @GameTest
    public void onlyBlocksMinedInZonesCount(GameTestHelper helper) {
        final var player = player(helper);
        final var chunk = addZoneChunk(helper, player, new ChunkPos(6020, 0));
        final var before = score(player, ResourceZonesStats.BLOCKS_MINED);

        ResourceZonesStats.recordBlockMined(helper.getLevel(), player, chunk.getMiddleBlockPosition(64));
        ResourceZonesStats.recordBlockMined(helper.getLevel(), player, new BlockPos(6021 * 16, 64, 0));

        helper.assertValueEqual(score(player, ResourceZonesStats.BLOCKS_MINED) - before, 1, "score increase");
        helper.succeed();
    }
}
