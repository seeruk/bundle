package dev.seeruk.bundle.digest.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.digest.Archive;
import dev.seeruk.bundle.digest.SeersDigest;
import dev.seeruk.bundle.digest.api.Issue;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestCommands.executeAsAdmin;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class DigestCommandGameTest {
    // The command uses the server's config and schedule, which other tests don't touch

    // Archives an issue where only the player was active, returning its number
    private static int archive(GameTestHelper helper, ServerPlayer player) {
        final var server = helper.getLevel().getServer();
        final var number = Archive.nextIssueNumber(server);
        final var at = Instant.parse("2026-11-22T18:00:00Z");
        Archive.add(server, new Issue(number, at, at, Map.of(), List.of(player.getUUID()), List.of(), Issue.Moments.EMPTY));
        return number;
    }

    // The cover of the book the player was given, as plain text
    private static String cover(ServerPlayer player) {
        return player.getInventory().getNonEquipmentItems().stream()
            .map(stack -> stack.get(DataComponents.WRITTEN_BOOK_CONTENT))
            .filter(Objects::nonNull)
            .findFirst()
            .map(content -> content.pages().getFirst().raw().getString())
            .orElse("");
    }

    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(root.getChild(DigestCommand.NAME) != null, "Command not registered as " + DigestCommand.NAME);
        helper.assertTrue(
            root.getChild(SeersDigest.MOD_ID + ":" + DigestCommand.NAME) != null,
            "Command not registered as " + SeersDigest.MOD_ID + ":" + DigestCommand.NAME
        );
        helper.succeed();
    }

    @GameTest
    public void publishAndListSucceed(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " publish"), 1, "publish result");
        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " list"), 1, "list result");
        helper.succeed();
    }

    @GameTest
    public void bookGivesTheNumberedIssue(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var number = archive(helper, player);
        // So the numbered issue isn't the latest
        archive(helper, player);

        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " book " + number), 1, "result");
        helper.assertTrue(cover(player).contains("issue #" + number), "Wrong issue given: " + cover(player));
        helper.succeed();
    }

    @GameTest
    public void bookGivesTheLatestIssueWithoutANumber(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var number = archive(helper, player);

        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " book"), 1, "result");
        helper.assertTrue(cover(player).contains("issue #" + number), "Wrong issue given: " + cover(player));
        helper.succeed();
    }

    @GameTest
    public void bookFailsForAnIssueThatDoesNotExist(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var number = Archive.nextIssueNumber(helper.getLevel().getServer()) + 1000;

        helper.assertValueEqual(executeAsAdmin(helper, player, DigestCommand.NAME + " book " + number), 0, "result");
        helper.assertTrue(player.getInventory().isEmpty(), "A book was given");
        helper.succeed();
    }

    @GameTest
    public void isOnlyForAdmins(GameTestHelper helper) {
        final var player = player(helper);

        try {
            execute(helper, player, DigestCommand.NAME + " list");
            helper.fail("Non-admin could use the command");
        } catch (CommandSyntaxException e) {
            // Expected
        }

        helper.succeed();
    }
}
