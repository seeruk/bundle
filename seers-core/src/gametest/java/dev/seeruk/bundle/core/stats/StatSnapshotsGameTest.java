package dev.seeruk.bundle.core.stats;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.stats.Stats;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class StatSnapshotsGameTest {
    // Snapshots are shared by the whole server, so each test uses its own holders and moments.

    private static final String JUMP = "minecraft.custom:minecraft.jump";

    // A moment nothing else holds a snapshot of
    private static Instant uniqueMoment() {
        return Instant.ofEpochSecond(UUID.randomUUID().getMostSignificantBits() >>> 16);
    }

    @GameTest
    public void holdTakesSnapshotOfCurrentStats(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var at = uniqueMoment();
        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 3);

        StatSnapshots.hold(server, "test:takes", at);

        // A snapshot is a copy, so stats gained after it's taken aren't in it
        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 2);

        final var snapshot = StatSnapshots.held(server, "test:takes");
        helper.assertValueEqual(snapshot.map(StatSnapshots.Snapshot::at), Optional.of(at), "moment");
        helper.assertValueEqual(
            snapshot.map(found -> found.values().getOrDefault(player.getUUID(), Map.of()).get(JUMP)),
            Optional.of(3),
            "jumps"
        );
        helper.succeed();
    }

    @GameTest
    public void holdersOfTheSameMomentShareSnapshot(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var at = uniqueMoment();

        // If not super clear, we're holding the same moment here, and below, and ensuring that the
        // second holder gets the result from the first, instead of recalculating something that
        // already exists before it should. To get a new value, this moment would need to be
        // released (and then automatically deleted), choose a different moment (besides the point
        // of this test), or move the first holder to a different moment (similar to release).
        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 3);
        StatSnapshots.hold(server, "test:shares_first", at);

        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 2);
        StatSnapshots.hold(server, "test:shares_second", at);

        helper.assertValueEqual(
            StatSnapshots.held(server, "test:shares_second").map(found -> found.values().get(player.getUUID()).get(JUMP)),
            Optional.of(3),
            "jumps"
        );
        helper.succeed();
    }

    @GameTest
    public void holdingAnotherMomentDeletesUnheldSnapshot(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);
        final var first = uniqueMoment();
        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 3);
        StatSnapshots.hold(server, "test:moves", first);

        // Moving the holder leaves nothing holding the first moment, so its snapshot is deleted
        StatSnapshots.hold(server, "test:moves", uniqueMoment());

        // Holding the first moment again takes a new snapshot, as the old one was deleted
        player.awardStat(Stats.CUSTOM.get(Stats.JUMP), 2);
        StatSnapshots.hold(server, "test:moves_again", first);

        helper.assertValueEqual(
            StatSnapshots.held(server, "test:moves_again").map(found -> found.values().get(player.getUUID()).get(JUMP)),
            Optional.of(5),
            "jumps"
        );
        helper.succeed();
    }

    @GameTest
    public void releaseStopsHolding(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        StatSnapshots.hold(server, "test:releases", uniqueMoment());

        StatSnapshots.release(server, "test:releases");

        helper.assertValueEqual(StatSnapshots.held(server, "test:releases"), Optional.empty(), "snapshot");
        helper.succeed();
    }
}
