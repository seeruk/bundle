package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.markdown.MarkdownRenderer;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class DiscordRelayGameTest {
    private static final DiscordConfig.DiscordFormats FORMATS = new DiscordConfig.DiscordFormats(
        new DiscordTemplate("${message}"),
        new DiscordTemplate("_${display_name} ${message}_")
    );

    @GameTest
    public void chatIsSentAsThePlayer(GameTestHelper helper) {
        final var player = player(helper);

        final var message = relay(player, ChatType.CHAT, Component.literal("Testing"));

        helper.assertValueEqual(
            message.map(WebhookMessage::username),
            Optional.of(player.getGameProfile().name()),
            "username"
        );
        helper.assertValueEqual(
            message.map(WebhookMessage::avatarUrl),
            Optional.of(DiscordRelay.avatarUrl(player.getUUID())),
            "avatar"
        );
        helper.assertValueEqual(message.map(WebhookMessage::content), Optional.of("Testing"), "content");
        helper.succeed();
    }

    @GameTest
    public void chatKeepsItsFormatting(GameTestHelper helper) {
        final var player = player(helper);

        final var message = relay(player, ChatType.CHAT, Component.literal("Testing").withStyle(ChatFormatting.BOLD));

        helper.assertValueEqual(message.map(WebhookMessage::content), Optional.of("**Testing**"), "content");
        helper.succeed();
    }

    @GameTest
    public void emotesUseTheirFormat(GameTestHelper helper) {
        final var player = player(helper);

        final var message = relay(player, ChatType.EMOTE_COMMAND, Component.literal("waves"));

        final var name = MarkdownRenderer.escape(player.getGameProfile().name());
        helper.assertValueEqual(message.map(WebhookMessage::content), Optional.of("_" + name + " waves_"), "content");
        helper.succeed();
    }

    @GameTest
    public void otherMessagesAreNotSent(GameTestHelper helper) {
        final var player = player(helper);

        helper.assertValueEqual(
            relay(player, ChatType.SAY_COMMAND, Component.literal("Testing")),
            Optional.empty(),
            "message"
        );
        helper.succeed();
    }

    @GameTest
    public void playersAreNamedAsChatShowsThem(GameTestHelper helper) {
        final var player = player(helper);
        final var scoreboard = helper.getLevel().getServer().getScoreboard();
        final var team = scoreboard.addPlayerTeam(player.getGameProfile().name());
        team.setPlayerPrefix(Component.literal("[Admin] "));
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);

        final var message = relay(player, ChatType.CHAT, Component.literal("Testing"));

        helper.assertValueEqual(
            message.map(WebhookMessage::username),
            Optional.of("[Admin] " + player.getGameProfile().name()),
            "username"
        );
        scoreboard.removePlayerTeam(team);
        helper.succeed();
    }

    @GameTest
    public void linkedItemsAreEmbedded(GameTestHelper helper) {
        final var player = player(helper);
        final var sword = new ItemStack(Items.DIAMOND_SWORD);

        final var message = relay(player, ChatType.CHAT, Component.literal("Look: ").append(sword.getDisplayName()));

        helper.assertValueEqual(
            message.map(WebhookMessage::embeds).map(embeds -> embeds.stream().map(MessageEmbed::getTitle).toList()),
            Optional.of(List.of("Diamond Sword")),
            "embed titles"
        );
        helper.succeed();
    }

    /**
     * The message for Discord, for the player sending the content as the given kind of message.
     */
    private static Optional<WebhookMessage> relay(ServerPlayer player, ResourceKey<ChatType> chatType, Component content) {
        final var message = PlayerChatMessage.system(content.getString()).withUnsignedContent(content);
        return DiscordRelay.message(message, ChatType.bind(chatType, player), player, FORMATS);
    }
}
