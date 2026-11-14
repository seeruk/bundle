package dev.seeruk.bundle.nicks.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.nicks.Nickname;
import dev.seeruk.bundle.nicks.Nicknames;
import dev.seeruk.bundle.nicks.SeersNicks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.nicks.NicksFixtures.nickname;
import static dev.seeruk.bundle.nicks.NicksFixtures.player;

public class NickCommandGameTest {
    @GameTest
    public void registersNameAndNamespacedName(GameTestHelper helper) {
        final var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();

        helper.assertTrue(
            root.getChild(NickCommand.NAME) != null,
            "Command not registered as " + NickCommand.NAME
        );

        helper.assertTrue(
            root.getChild(SeersNicks.MOD_ID + ":" + NickCommand.NAME) != null,
            "Command not registered as " + SeersNicks.MOD_ID + ":" + NickCommand.NAME
        );

        helper.succeed();
    }

    @GameTest
    public void setsOwnNickname(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        final var nickname = nickname("Seer is ");

        // We can also cheekily check that spaces are supported here too...
        final var result = execute(helper, player, NickCommand.NAME + " set " + nickname);

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(nicknameFor(player), Optional.of(nickname), "nickname");
        helper.succeed();
    }

    @GameTest
    public void resetsOwnNickname(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        execute(helper, player, NickCommand.NAME + " set " + nickname("Seer"));

        final var result = execute(helper, player, NickCommand.NAME + " reset");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(nicknameFor(player), Optional.empty(), "nickname");
        helper.succeed();
    }

    @GameTest
    public void subcommandNamesCanBeNicknames(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);
        final var nickname = nickname("reset ");

        execute(helper, player, NickCommand.NAME + " set " + nickname);

        helper.assertValueEqual(nicknameFor(player), Optional.of(nickname), "nickname");
        helper.succeed();
    }

    @GameTest
    public void failedEditReturnsZero(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        final var result = execute(helper, player, NickCommand.NAME + " set " + "a".repeat(Nickname.MAX_LENGTH+1));

        helper.assertValueEqual(result, 0, "command result");
        helper.assertValueEqual(nicknameFor(player), Optional.empty(), "nickname");
        helper.succeed();
    }

    @GameTest
    public void adminsCanSetOtherPlayersNicknames(GameTestHelper helper) throws CommandSyntaxException {
        final var admin = player(helper);
        final var target = player(helper);

        final var nickname = nickname("Seer");

        final var result = executeAsAdmin(helper, admin, NickCommand.NAME + " player " + selector(target) + " set <b>" + nickname + "</b>");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(nicknameFor(target), Optional.of(nickname), "nickname");
        helper.succeed();
    }

    @GameTest
    public void adminsCanResetOtherPlayersNicknames(GameTestHelper helper) throws CommandSyntaxException {
        final var admin = player(helper);
        final var target = player(helper);
        execute(helper, target, NickCommand.NAME + " set " + nickname("Seer"));

        final var result = executeAsAdmin(helper, admin, NickCommand.NAME + " player " + selector(target) + " reset");

        helper.assertValueEqual(result, 1, "command result");
        helper.assertValueEqual(nicknameFor(target), Optional.empty(), "nickname");
        helper.succeed();
    }

    @GameTest
    public void onlyAdminsCanUsePlayerSubcommand(GameTestHelper helper) {
        final var player = player(helper);
        final var subcommand = helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
            .getChild(NickCommand.NAME)
            .getChild("player");

        helper.assertTrue(!subcommand.canUse(player.createCommandSourceStack()), "Player could use it");
        helper.assertTrue(subcommand.canUse(adminSource(player)), "Admin couldn't use it");
        helper.succeed();
    }

    @GameTest
    public void refusesNonPlayers(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var source = server.createCommandSourceStack().withSuppressedOutput();

        try {
            server.getCommands().getDispatcher().execute(NickCommand.NAME + " set Seer", source);
            helper.fail("Command didn't fail for a non-player");
        } catch (CommandSyntaxException e) {
            helper.succeed();
        }
    }

    private static int executeAsAdmin(GameTestHelper helper, ServerPlayer player, String command) throws CommandSyntaxException {
        final var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        return dispatcher.execute(command, adminSource(player));
    }

    private static CommandSourceStack adminSource(ServerPlayer player) {
        return player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
    }

    // Mock players all share the same name, and the GameTest server makes up a player for any name
    // it doesn't know, so this picks the player out with a selector instead
    private static String selector(ServerPlayer player) {
        final var tag = "nicks_" + player.getUUID().toString().substring(0, 8);
        player.addTag(tag);
        return "@a[tag=" + tag + "]";
    }

    private static Optional<String> nicknameFor(ServerPlayer player) {
        return Nicknames.get(player.getUUID()).map(Component::getString);
    }
}
