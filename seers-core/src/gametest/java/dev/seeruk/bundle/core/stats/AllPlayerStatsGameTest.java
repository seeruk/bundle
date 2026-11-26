package dev.seeruk.bundle.core.stats;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.ScoreHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

/**
 * Offline players are written as stats files with a random UUID, and removed again afterwards.
 */
public class AllPlayerStatsGameTest {
    private static final String JUMP = "minecraft.custom:minecraft.jump";

    private static Path statsFile(MinecraftServer server, UUID id) throws IOException {
        final var folder = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Files.createDirectories(folder);
        return folder.resolve(id + ".json");
    }

    private static void writeJumps(Path file, int jumps, FileTime modified) throws IOException {
        Files.writeString(
            file, """
                {"stats": {"minecraft:custom": {"minecraft:jump": %d}}, "DataVersion": %d}
                """.formatted(jumps, SharedConstants.getCurrentVersion().dataVersion().version())
        );
        Files.setLastModifiedTime(file, modified);
    }

    private static Integer value(MinecraftServer server, UUID id, String name) {
        return AllPlayerStats.read(server).getOrDefault(id, Map.of()).get(name);
    }

    @GameTest
    public void readReadsOnlinePlayersStats(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);

        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 3);

        helper.assertValueEqual(value(server, player.getUUID(), JUMP), 3, "jumps");
        helper.succeed();
    }

    @GameTest
    public void readReadsOfflinePlayersStatsFiles(GameTestHelper helper) throws IOException {
        final var server = helper.getLevel().getServer();
        final var id = UUID.randomUUID();
        final var file = statsFile(server, id);

        try {
            writeJumps(file, 7, FileTime.fromMillis(1_000_000));
            helper.assertValueEqual(value(server, id, JUMP), 7, "jumps");
        } finally {
            Files.deleteIfExists(file);
        }

        helper.succeed();
    }

    @GameTest
    public void readOnlyRereadsStatsFilesOnceTheyChange(GameTestHelper helper) throws IOException {
        final var server = helper.getLevel().getServer();
        final var id = UUID.randomUUID();
        final var file = statsFile(server, id);

        try {
            writeJumps(file, 7, FileTime.fromMillis(1_000_000));
            value(server, id, JUMP);

            writeJumps(file, 8, FileTime.fromMillis(1_000_000));
            helper.assertValueEqual(value(server, id, JUMP), 7, "jumps while unchanged");

            writeJumps(file, 8, FileTime.fromMillis(2_000_000));
            helper.assertValueEqual(value(server, id, JUMP), 8, "jumps once saved again");
        } finally {
            Files.deleteIfExists(file);
        }

        helper.succeed();
    }

    @GameTest
    public void readIncludesOnlinePlayersScores(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var objective = addObjective(server, "all_player_stats_online");

        try {
            server.getScoreboard().getOrCreatePlayerScore(player, objective).set(5);
            helper.assertValueEqual(value(server, player.getUUID(), "scoreboard:all_player_stats_online"), 5, "score");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void readIncludesOfflinePlayersScoresByCachedName(GameTestHelper helper) throws IOException {
        final var server = helper.getLevel().getServer();
        final var player = new NameAndId(UUID.randomUUID(), "offline_" + UUID.randomUUID().toString().substring(0, 8));
        final var file = statsFile(server, player.id());
        final var objective = addObjective(server, "all_player_stats_offline");

        try {
            writeJumps(file, 1, FileTime.fromMillis(1_000_000));
            server.services().nameToIdCache().add(player);
            server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.name()), objective).set(9);

            helper.assertValueEqual(value(server, player.id(), "scoreboard:all_player_stats_offline"), 9, "score");
        } finally {
            server.getScoreboard().removeObjective(objective);
            Files.deleteIfExists(file);
        }

        helper.succeed();
    }

    @GameTest
    public void readLeavesOutScoresForOtherHolders(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "all_player_stats_other");

        try {
            server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly("#not_a_player"), objective).set(4);

            final var scored = AllPlayerStats.read(server).values().stream()
                .anyMatch(values -> values.containsKey("scoreboard:all_player_stats_other"));
            helper.assertTrue(!scored, "A score for something other than a player was included");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
