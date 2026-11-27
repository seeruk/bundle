package dev.seeruk.bundle.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.chat.ChatFixtures.chat;
import static dev.seeruk.bundle.chat.ChatFixtures.onlySentBy;
import static dev.seeruk.bundle.chat.ChatFixtures.styles;
import static dev.seeruk.bundle.core.testing.TestCommands.execute;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class MessageFormatterGameTest {
    @GameTest
    public void chatMessagesAreFormatted(GameTestHelper helper) {
        final var player = player(helper);

        chat(player, "<red>Testing</red>");

        helper.succeedWhen(() -> {
            final var message = onlySentBy(helper, player).message();
            helper.assertValueEqual(message.decoratedContent().getString(), "Testing", "message text");
            helper.assertValueEqual(
                styles(message.decoratedContent()),
                List.of(Style.EMPTY.withColor(ChatFormatting.RED)),
                "message styles"
            );
        });
    }

    @GameTest
    public void plainChatMessagesAreLeftUnchanged(GameTestHelper helper) {
        // Clients mark messages the server changed, so messages with nothing to format mustn't be
        final var player = player(helper);

        chat(player, "Testing");

        helper.succeedWhen(() -> helper.assertTrue(
            onlySentBy(helper, player).message().unsignedContent() == null,
            "Message was changed"
        ));
    }

    @GameTest
    public void unsafeTagsAreNotParsed(GameTestHelper helper) {
        final var player = player(helper);

        chat(player, "<run_cmd /seed>Testing</run_cmd>");

        helper.succeedWhen(() -> {
            final var styles = styles(onlySentBy(helper, player).message().decoratedContent());
            helper.assertTrue(
                styles.stream().allMatch(style -> style.getClickEvent() == null),
                "Message has a click event"
            );
        });
    }

    @GameTest
    public void itemTagLinksTheHeldItem(GameTestHelper helper) {
        final var player = player(helper);
        final var stack = new ItemStack(Items.DIAMOND_SWORD);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        chat(player, "Look at <item>");

        helper.succeedWhen(() -> {
            final var content = onlySentBy(helper, player).message().decoratedContent();
            helper.assertValueEqual(
                content.getString(),
                "Look at " + stack.getDisplayName().getString(),
                "message text"
            );

            final var shown = styles(content).stream()
                .map(Style::getHoverEvent)
                .filter(event -> event instanceof HoverEvent.ShowItem)
                .map(event -> ((HoverEvent.ShowItem) event).item().item().value())
                .distinct()
                .toList();
            helper.assertValueEqual(shown, List.of(Items.DIAMOND_SWORD), "items shown");
        });
    }

    @GameTest
    public void itemTagIsLeftAsWrittenWithAnEmptyHand(GameTestHelper helper) {
        // The parser builds this from more than one piece of text, so the message only looks
        // unchanged to clients if it's left as it was
        final var player = player(helper);

        chat(player, "Look at <item>");

        helper.succeedWhen(() -> {
            final var message = onlySentBy(helper, player).message();
            helper.assertValueEqual(message.decoratedContent().getString(), "Look at <item>", "message text");
            helper.assertTrue(message.unsignedContent() == null, "Message was changed");
        });
    }

    @GameTest
    public void emotesAreFormatted(GameTestHelper helper) throws CommandSyntaxException {
        final var player = player(helper);

        execute(helper, player, "me <red>waves</red>");

        helper.succeedWhen(() -> {
            final var sent = onlySentBy(helper, player);
            helper.assertValueEqual(sent.chatType(), ChatType.EMOTE_COMMAND, "chat type");
            helper.assertValueEqual(
                styles(sent.message().decoratedContent()),
                List.of(Style.EMPTY.withColor(ChatFormatting.RED)),
                "message styles"
            );
        });
    }

    @GameTest
    public void trustedPlayersCanUseEveryTag(GameTestHelper helper) {
        final var player = trusted(helper, player(helper));

        chat(player, "<run_cmd /seed>Testing</run_cmd>");

        helper.succeedWhen(() -> {
            final var styles = styles(onlySentBy(helper, player).message().decoratedContent());
            helper.assertTrue(
                styles.stream().anyMatch(style -> style.getClickEvent() instanceof ClickEvent.RunCommand),
                "Message has no click event"
            );
        });
    }

    @GameTest
    public void trustedPlayersCanUsePlaceholders(GameTestHelper helper) {
        final var player = trusted(helper, player(helper));

        chat(player, "I'm %player:name%");

        helper.succeedWhen(() -> helper.assertValueEqual(
            onlySentBy(helper, player).message().decoratedContent().getString(),
            "I'm " + player.getGameProfile().name(),
            "message text"
        ));
    }

    @GameTest
    public void untrustedPlayersCannotUsePlaceholders(GameTestHelper helper) {
        final var player = player(helper);

        chat(player, "I'm %player:name%");

        helper.succeedWhen(() -> helper.assertValueEqual(
            onlySentBy(helper, player).message().decoratedContent().getString(),
            "I'm %player:name%",
            "message text"
        ));
    }

    /**
     * Makes the player an operator who can use commands like {@code /say}. The GameTest server's
     * operators can't use any commands by default.
     */
    private static ServerPlayer trusted(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().op(
            player.nameAndId(),
            Optional.of(LevelBasedPermissionSet.GAMEMASTER),
            Optional.empty()
        );
        return player;
    }
}
