package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.discord.config.DiscordConfig;
import dev.seeruk.bundle.discord.config.EventsConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.IntStream;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class AdvancementBatcherGameTest {
    // The defaults, from the dist config
    private static final EventsConfig EVENTS = ModConfig.load(SeersDiscord.MOD_ID, DiscordConfig.CODEC)
        .discord().events();

    @GameTest
    public void advancementsCompletedTogetherAreSentInOneMessage(GameTestHelper helper) {
        final var player = player(helper);
        final var batcher = new AdvancementBatcher();

        complete(batcher, player, 3);

        final var messages = batcher.flush(EVENTS);
        helper.assertValueEqual(messages.size(), 1, "messages");
        helper.assertValueEqual(
            titles(messages.getFirst()),
            List.of("Advancement 0", "Advancement 1", "Advancement 2"),
            "embeds"
        );
        helper.succeed();
    }

    @GameTest
    public void asManyAdvancementsAsFitInOneMessageAreSentInOne(GameTestHelper helper) {
        final var player = player(helper);
        final var batcher = new AdvancementBatcher();

        complete(batcher, player, Message.MAX_EMBED_COUNT);

        final var messages = batcher.flush(EVENTS);
        helper.assertValueEqual(messages.size(), 1, "messages");
        helper.assertValueEqual(messages.getFirst().embeds().size(), Message.MAX_EMBED_COUNT, "embeds");
        helper.succeed();
    }

    @GameTest
    public void moreAdvancementsThanFitInOneMessageAreSummarised(GameTestHelper helper) {
        final var player = player(helper);
        final var batcher = new AdvancementBatcher();
        final var count = Message.MAX_EMBED_COUNT + 1;

        complete(batcher, player, count);

        final var messages = batcher.flush(EVENTS);
        helper.assertValueEqual(messages.size(), 1, "messages");
        helper.assertValueEqual(
            titles(messages.getFirst()),
            List.of(player.getGameProfile().name() + " completed " + count + " advancements"),
            "embeds"
        );
        helper.succeed();
    }

    @GameTest
    public void advancementsWithTooMuchTextForOneMessageAreSummarised(GameTestHelper helper) {
        final var player = player(helper);
        final var batcher = new AdvancementBatcher();

        // Two are fewer than fit by count, but they have more text between them than Discord allows
        final var description = "a".repeat(MessageEmbed.EMBED_MAX_LENGTH_BOT / 2);
        batcher.add(player, new EmbedBuilder().setAuthor("One").setDescription(description).build());
        batcher.add(player, new EmbedBuilder().setAuthor("Two").setDescription(description).build());

        final var messages = batcher.flush(EVENTS);
        helper.assertValueEqual(
            titles(messages.getFirst()),
            List.of(player.getGameProfile().name() + " completed 2 advancements"),
            "embeds"
        );
        helper.succeed();
    }

    @GameTest
    public void eachPlayersAdvancementsAreSentSeparately(GameTestHelper helper) {
        final var batcher = new AdvancementBatcher();

        complete(batcher, player(helper), 2);
        complete(batcher, player(helper), 1);

        final var messages = batcher.flush(EVENTS);
        helper.assertValueEqual(
            messages.stream().map(message -> message.embeds().size()).toList(),
            List.of(2, 1),
            "embeds per message"
        );
        helper.succeed();
    }

    @GameTest
    public void advancementsAreOnlySentOnce(GameTestHelper helper) {
        final var batcher = new AdvancementBatcher();

        complete(batcher, player(helper), 2);
        batcher.flush(EVENTS);

        helper.assertValueEqual(batcher.flush(EVENTS), List.of(), "messages");
        helper.succeed();
    }

    private static void complete(AdvancementBatcher batcher, ServerPlayer player, int count) {
        IntStream.range(0, count).forEach(i ->
            batcher.add(player, new EmbedBuilder().setAuthor("Advancement " + i).build()));
    }

    private static List<String> titles(WebhookMessage message) {
        return message.embeds().stream().map(embed -> embed.getAuthor().getName()).toList();
    }
}
