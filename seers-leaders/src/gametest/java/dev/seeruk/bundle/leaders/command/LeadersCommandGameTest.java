package dev.seeruk.bundle.leaders.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.leaders.LeaderMannequins;
import dev.seeruk.bundle.leaders.Seat;
import dev.seeruk.bundle.leaders.SeersLeaders;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestCommands.executeAsAdmin;
import static dev.seeruk.bundle.leaders.LeadersFixtures.BLOCK;
import static dev.seeruk.bundle.leaders.LeadersFixtures.playerFacingBlock;

/**
 * The command uses the server's config, so these use its default {@code miner} category.
 */
public class LeadersCommandGameTest {
    private static List<Mannequin> mannequinsOnBlock(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Mannequin.class, new AABB(helper.absolutePos(BLOCK.above())));
    }

    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(root.getChild(LeadersCommand.NAME) != null, "Command not registered as " + LeadersCommand.NAME);
        helper.assertTrue(
            root.getChild(SeersLeaders.MOD_ID + ":" + LeadersCommand.NAME) != null,
            "Command not registered as " + SeersLeaders.MOD_ID + ":" + LeadersCommand.NAME
        );
        helper.succeed();
    }

    @GameTest
    public void placePlacesMannequinForTheSeat(GameTestHelper helper) throws CommandSyntaxException {
        final var player = playerFacingBlock(helper);

        final var result = executeAsAdmin(helper, player, LeadersCommand.NAME + " place miner 2");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(
            mannequinsOnBlock(helper).stream().map(LeaderMannequins::seat).toList(),
            List.of(Optional.of(new Seat("miner", 2))),
            "seats"
        );
        helper.succeed();
    }

    @GameTest
    public void placeDefaultsToFirstPlace(GameTestHelper helper) throws CommandSyntaxException {
        final var player = playerFacingBlock(helper);

        executeAsAdmin(helper, player, LeadersCommand.NAME + " place miner");

        helper.assertValueEqual(
            mannequinsOnBlock(helper).stream().map(LeaderMannequins::seat).toList(),
            List.of(Optional.of(new Seat("miner", 1))),
            "seats"
        );
        helper.succeed();
    }

    @GameTest
    public void placeFailsForUnknownCategory(GameTestHelper helper) throws CommandSyntaxException {
        final var player = playerFacingBlock(helper);

        final var result = executeAsAdmin(helper, player, LeadersCommand.NAME + " place missing");

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(mannequinsOnBlock(helper), List.of(), "mannequins");
        helper.succeed();
    }

    @GameTest
    public void removeRemovesLookedAtMannequin(GameTestHelper helper) throws CommandSyntaxException {
        final var player = playerFacingBlock(helper);
        executeAsAdmin(helper, player, LeadersCommand.NAME + " place miner");
        final var mannequin = mannequinsOnBlock(helper).getFirst();
        player.lookAt(EntityAnchorArgument.Anchor.EYES, mannequin.getBoundingBox().getCenter());

        final var result = executeAsAdmin(helper, player, LeadersCommand.NAME + " remove");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertTrue(mannequin.isRemoved(), "Mannequin wasn't removed");
        helper.succeed();
    }

    @GameTest
    public void isOnlyForAdmins(GameTestHelper helper) {
        final var player = playerFacingBlock(helper);

        try {
            execute(helper, player, LeadersCommand.NAME + " place miner");
            helper.fail("Non-admin could use the command");
        } catch (CommandSyntaxException e) {
            helper.assertValueEqual(mannequinsOnBlock(helper), List.of(), "mannequins");
        }

        helper.succeed();
    }
}
