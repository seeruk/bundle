package dev.seeruk.bundle.leaders;

import dev.seeruk.bundle.leaders.mixin.MannequinInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.StatScores.setScore;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.core.testing.TestTimes.*;
import static dev.seeruk.bundle.leaders.LeadersFixtures.*;

public class LeaderMannequinsGameTest {
    private static Mannequin placed(GameTestHelper helper, PlaceResult result) {
        if (result instanceof PlaceResult.Placed placed) {
            return placed.mannequin();
        }

        helper.fail("Mannequin wasn't placed: " + result);
        return null;
    }

    private static String description(Mannequin mannequin) {
        return ((MannequinInvoker) mannequin).invokeGetDescription().getString();
    }

    private static Optional<UUID> profileId(Mannequin mannequin) {
        final var id = mannequin.getProfile().partialProfile().id();
        return id.equals(Mannequin.DEFAULT_PROFILE.partialProfile().id()) ? Optional.empty() : Optional.of(id);
    }

    @GameTest
    public void placeStandsOnTheBlockFacingThePlayer(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_place");
        final var player = playerFacingBlock(helper);

        try {
            final var mannequin = placed(
                helper,
                LeaderMannequins.place(player, config("place", objective), new Seat("place", 1))
            );

            helper.assertValueEqual(
                mannequin.position(),
                Vec3.atBottomCenterOf(helper.absolutePos(BLOCK.above())),
                "position"
            );
            helper.assertValueEqual(mannequin.getYRot(), 0f, "facing");
            helper.assertTrue(mannequin.isPermanentlyInvulnerable(), "Mannequin isn't invulnerable");
            helper.assertValueEqual(LeaderMannequins.seat(mannequin), Optional.of(new Seat("place", 1)), "seat");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void placeShowsWhoIsInTheSeat(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_shows");
        final var config = config("shows", objective);
        final var player = playerFacingBlock(helper);

        try {
            setScore(player, objective, 5);
            Leaders.refresh(server, config);

            final var mannequin = placed(helper, LeaderMannequins.place(player, config, new Seat("shows", 1)));

            helper.assertValueEqual(mannequin.getCustomName().getString(), "Test #1", "name");
            helper.assertValueEqual(description(mannequin), player.getGameProfile().name() + " - 5", "description");
            helper.assertValueEqual(profileId(mannequin), Optional.of(player.getUUID()), "profile");
            helper.assertTrue(
                mannequin.getMainHandItem().is(Items.STICK),
                "Mannequin isn't holding the category's item"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void placeShowsEmptySeat(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_empty");
        final var config = config("empty", objective);
        final var player = playerFacingBlock(helper);

        try {
            setScore(player, objective, 5);
            Leaders.refresh(server, config);

            final var mannequin = placed(helper, LeaderMannequins.place(player, config, new Seat("empty", 2)));

            helper.assertValueEqual(mannequin.getCustomName().getString(), "Test #2", "name");
            helper.assertValueEqual(description(mannequin), "Nobody yet", "description");
            helper.assertValueEqual(profileId(mannequin), Optional.empty(), "profile");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void placeFailsForUnknownCategory(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_unknown");
        final var player = playerFacingBlock(helper);

        try {
            final var result = LeaderMannequins.place(player, config("unknown", objective), new Seat("missing", 1));
            helper.assertValueEqual(result, new PlaceResult.UnknownCategory("missing"), "result");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void placeFailsWithoutBlockInSight(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_no_block");
        final var player = playerFacingBlock(helper);
        player.setXRot(-90);

        try {
            final var result = LeaderMannequins.place(player, config("no_block", objective), new Seat("no_block", 1));
            helper.assertValueEqual(result, new PlaceResult.NoBlockInSight(), "result");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void placeFailsWhenObstructed(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_obstructed");
        final var player = playerFacingBlock(helper);
        helper.setBlock(BLOCK.above(), Blocks.STONE);

        try {
            final var result = LeaderMannequins.place(
                player,
                config("obstructed", objective),
                new Seat("obstructed", 1)
            );
            helper.assertValueEqual(result, new PlaceResult.Obstructed(), "result");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void removeRemovesLookedAtMannequin(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_remove");
        final var player = playerFacingBlock(helper);

        try {
            final var mannequin = placed(
                helper,
                LeaderMannequins.place(player, config("remove", objective), new Seat("remove", 3))
            );
            player.lookAt(EntityAnchorArgument.Anchor.EYES, mannequin.getBoundingBox().getCenter());

            helper.assertValueEqual(
                LeaderMannequins.remove(player),
                new RemoveResult.Removed(new Seat("remove", 3)),
                "result"
            );
            helper.assertTrue(mannequin.isRemoved(), "Mannequin wasn't removed");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void removeLeavesOtherMannequins(GameTestHelper helper) {
        final var player = playerFacingBlock(helper);
        final var mannequin = EntityTypes.MANNEQUIN.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        final var position = Vec3.atBottomCenterOf(helper.absolutePos(BLOCK.above()));
        mannequin.snapTo(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(mannequin);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, mannequin.getBoundingBox().getCenter());

        helper.assertValueEqual(
            LeaderMannequins.remove(player),
            new RemoveResult.NotLookingAtLeaderMannequin(),
            "result"
        );
        helper.assertTrue(!mannequin.isRemoved(), "Mannequin was removed");
        helper.succeed();
    }

    @GameTest
    public void refreshShowsNewLeader(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_refresh");
        final var config = config("refresh", objective);
        final var player = playerFacingBlock(helper);
        final var overtaker = player(helper);

        try {
            setScore(player, objective, 5);
            Leaders.refresh(server, config);
            final var mannequin = placed(helper, LeaderMannequins.place(player, config, new Seat("refresh", 1)));

            setScore(overtaker, objective, 6);
            Leaders.refresh(server, config);

            helper.assertValueEqual(profileId(mannequin), Optional.of(overtaker.getUUID()), "profile");
            helper.assertValueEqual(description(mannequin), overtaker.getGameProfile().name() + " - 6", "description");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void loadedMannequinShowsWhoIsInItsSeat(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_loaded");
        final var config = config("loaded", objective);
        final var player = playerFacingBlock(helper);

        try {
            // Unloading stops refreshes updating it, as it's no longer tracked
            final var mannequin = placed(helper, LeaderMannequins.place(player, config, new Seat("loaded", 1)));
            LeaderMannequins.unloaded(mannequin);

            setScore(player, objective, 5);
            Leaders.refresh(server, config);
            helper.assertValueEqual(description(mannequin), "Nobody yet", "description while unloaded");

            LeaderMannequins.loaded(mannequin, config);
            helper.assertValueEqual(
                description(mannequin),
                player.getGameProfile().name() + " - 5",
                "description once loaded"
            );
            helper.assertValueEqual(profileId(mannequin), Optional.of(player.getUUID()), "profile once loaded");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void emptyPeriodicSeatShowsWhoHeldItLastPeriod(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_previous");
        final var config = periodConfig("previous", objective, DAILY);
        final var player = playerFacingBlock(helper);

        try {
            final var day = uniqueDay();
            Periods.check(server, config, at(day));
            setScore(player, objective, 5);
            final var mannequin = placed(helper, LeaderMannequins.place(player, config, new Seat("previous", 1)));

            // Ending the period empties the seat, so the mannequin shows who held it instead (from
            // the config's previous_description), but not their skin, as they don't hold it now
            Periods.check(server, config, at(day.plusDays(1)));

            helper.assertValueEqual(description(mannequin), "Last: " + player.getGameProfile().name(), "description");
            helper.assertValueEqual(profileId(mannequin), Optional.empty(), "profile");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void survivalPlayersCannotHurtLeaderMannequins(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "mannequins_hurt");
        final var player = playerFacingBlock(helper);

        try {
            final var mannequin = placed(
                helper,
                LeaderMannequins.place(player, config("hurt", objective), new Seat("hurt", 1))
            );
            final var hurt = mannequin.hurtServer(
                helper.getLevel(),
                helper.getLevel().damageSources().playerAttack(player),
                100
            );

            helper.assertTrue(!hurt, "Mannequin was hurt");
            helper.assertTrue(mannequin.isAlive(), "Mannequin died");
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
