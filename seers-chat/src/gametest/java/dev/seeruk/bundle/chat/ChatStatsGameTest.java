package dev.seeruk.bundle.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.stats.ScoreboardStat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

import java.util.List;

import static dev.seeruk.bundle.chat.ChatFixtures.chat;
import static dev.seeruk.bundle.core.testing.StatScores.score;
import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class ChatStatsGameTest {
    private static final List<ScoreboardStat> STATS = List.of(
        ChatStats.MESSAGES_SENT,
        ChatStats.MESSAGE_CHARACTERS,
        ChatStats.EMOTES_SENT,
        ChatStats.EMOTE_CHARACTERS
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

        for (final var stat : STATS) {
            try {
                dispatcher.execute("scoreboard objectives setdisplay sidebar " + stat.objectiveId(), source);
                dispatcher.execute("scoreboard objectives setdisplay sidebar", source);
            } catch (CommandSyntaxException e) {
                helper.fail("Objective " + stat.objectiveId() + " couldn't be used in a command: " + e.getMessage());
            }
        }

        helper.succeed();
    }

    @GameTest
    public void chatMessagesCountWithoutTags(GameTestHelper helper) {
        final var player = player(helper);

        chat(player, "<red>Testing</red> 1");

        helper.succeedWhen(() -> {
            helper.assertValueEqual(score(player, ChatStats.MESSAGES_SENT), 1, "messages sent");
            helper.assertValueEqual(score(player, ChatStats.MESSAGE_CHARACTERS), 9, "message characters");
            helper.assertValueEqual(score(player, ChatStats.EMOTES_SENT), 0, "emotes sent");
        });
    }

    @GameTest
    public void emotesCountWithoutTags(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        execute(helper, player, "me <red>waves</red>");

        helper.succeedWhen(() -> {
            helper.assertValueEqual(score(player, ChatStats.EMOTES_SENT), 1, "emotes sent");
            helper.assertValueEqual(score(player, ChatStats.EMOTE_CHARACTERS), 5, "emote characters");
            helper.assertValueEqual(score(player, ChatStats.MESSAGES_SENT), 0, "messages sent");
        });
    }

    @GameTest
    public void otherCommandMessagesDoNotCount(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();

        helper.getLevel().getServer().getCommands().getDispatcher().execute("say Testing", source);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(ChatFixtures.sentBy(player).size(), 1, "messages sent");
            helper.assertValueEqual(score(player, ChatStats.EMOTES_SENT), 0, "emotes sent");
            helper.assertValueEqual(score(player, ChatStats.MESSAGES_SENT), 0, "messages sent");
        });
    }
}
