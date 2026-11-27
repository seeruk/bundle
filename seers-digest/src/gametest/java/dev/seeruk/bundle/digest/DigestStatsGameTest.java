package dev.seeruk.bundle.digest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Set;

import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestTimes.at;

public class DigestStatsGameTest {
    private static final NightHours NIGHT = new NightHours(LocalTime.MIDNIGHT, LocalTime.of(6, 0));

    @GameTest
    public void goingToTheNetherOrTheEndCountsAVisit(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);

        final var nether = Objects.requireNonNull(server.getLevel(Level.NETHER));
        player.teleportTo(nether, 0, 128, 0, Set.of(), 0, 0, false);
        final var end = Objects.requireNonNull(server.getLevel(Level.END));
        player.teleportTo(end, 0, 128, 0, Set.of(), 0, 0, false);

        helper.assertValueEqual(score(player, DigestStats.NETHER_VISITS), 1, "Nether visits");
        helper.assertValueEqual(score(player, DigestStats.END_VISITS), 1, "End visits");
        helper.succeed();
    }

    @GameTest
    public void timePlayedAtNightIsCounted(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var day = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        // A second at 3am counts, but a second at noon doesn't
        DigestStats.recordNightSecond(server, NIGHT, at(day.withHour(3)));
        DigestStats.recordNightSecond(server, NIGHT, at(day.withHour(12)));

        helper.assertValueEqual(score(player, DigestStats.NIGHT_TICKS), 20, "night ticks");
        helper.succeed();
    }
}
