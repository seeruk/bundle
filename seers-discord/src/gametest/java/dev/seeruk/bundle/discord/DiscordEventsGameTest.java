package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.discord.config.DiscordConfig;
import dev.seeruk.bundle.discord.config.EventsConfig;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class DiscordEventsGameTest {
    // The defaults, from the dist config
    private static final EventsConfig EVENTS = ModConfig.load(SeersDiscord.MOD_ID, DiscordConfig.CODEC)
        .discord().events();

    private static final int YELLOW = 0xFFFF55;

    @GameTest
    public void joiningIsSentWithThePlayersFace(GameTestHelper helper) {
        final var player = player(helper);

        final var message = DiscordRelay.joined(player, EVENTS).orElseThrow();
        final var embed = message.embeds().getFirst();

        // Sent as the webhook itself, rather than as the player
        helper.assertTrue(message.username() == null, "Should be sent as the webhook");
        helper.assertValueEqual(embed.getAuthor().getName(), name(player) + " joined the game", "line");
        helper.assertValueEqual(embed.getAuthor().getIconUrl(), DiscordRelay.avatarUrl(player.getUUID()), "face");
        helper.assertValueEqual(embed.getColorRaw(), YELLOW, "colour");
        helper.succeed();
    }

    @GameTest
    public void leavingIsSent(GameTestHelper helper) {
        final var player = player(helper);

        helper.assertValueEqual(line(DiscordRelay.left(player, EVENTS)), name(player) + " left the game", "line");
        helper.succeed();
    }

    @GameTest
    public void eventsThatAreTurnedOffAreNotSent(GameTestHelper helper) {
        final var player = player(helper);
        final var join = EVENTS.join();
        final var events = withJoin(new EventsConfig.EventFormat(false, join.format(), join.colour()));

        helper.assertValueEqual(DiscordRelay.joined(player, events), Optional.empty(), "message");
        helper.succeed();
    }

    @GameTest
    public void eventsWithNothingToShowAreNotSent(GameTestHelper helper) {
        final var player = player(helper);
        final var empty = new EventsConfig.EventFormat(true, new DiscordTemplate(""), EVENTS.join().colour());
        final var events = withJoin(empty);

        helper.assertValueEqual(DiscordRelay.joined(player, events), Optional.empty(), "message");
        helper.succeed();
    }

    @GameTest
    public void deathsAreSentWithTheGamesDeathMessage(GameTestHelper helper) {
        final var player = player(helper);
        final var death = Component.literal(name(player) + " was slain by Zombie");

        final var embed = DiscordRelay.died(player, death, EVENTS).orElseThrow().embeds().getFirst();

        helper.assertValueEqual(embed.getAuthor().getName(), name(player) + " was slain by Zombie", "line");
        helper.assertValueEqual(embed.getColorRaw(), 0xFF5555, "colour");
        helper.succeed();
    }

    @GameTest
    public void advancementsAreSentWithTheGamesAnnouncementAndDescription(GameTestHelper helper) {
        final var player = player(helper);

        final var embed = advancement(helper, player, "story/mine_stone", EVENTS)
            .orElseThrow();

        helper.assertValueEqual(
            embed.getAuthor().getName(),
            name(player) + " has made the advancement [Stone Age]",
            "line"
        );
        helper.assertValueEqual(embed.getDescription(), "Mine Stone with your new Pickaxe", "description");
        helper.assertValueEqual(embed.getColorRaw(), 0x55FF55, "colour");
        helper.succeed();
    }

    @GameTest
    public void challengesUseTheChallengeColour(GameTestHelper helper) {
        final var player = player(helper);

        final var embed = advancement(helper, player, "adventure/sniper_duel", EVENTS)
            .orElseThrow();

        helper.assertValueEqual(embed.getColorRaw(), 0xAA00AA, "colour");
        helper.succeed();
    }

    @GameTest
    public void advancementDescriptionsAreEscaped(GameTestHelper helper) {
        // Player names in GameTests have an underscore, which Discord would read as italics
        final var player = player(helper);
        final var events = withDescription("${display_name}");

        final var embed = advancement(helper, player, "story/mine_stone", events)
            .orElseThrow();

        helper.assertValueEqual(embed.getDescription(), name(player).replace("_", "\\_"), "description");
        helper.succeed();
    }

    @GameTest
    public void advancementsWithoutADescriptionFormatHaveNoDescription(GameTestHelper helper) {
        final var player = player(helper);

        final var embed = advancement(helper, player, "story/mine_stone", withDescription(""))
            .orElseThrow();

        helper.assertTrue(embed.getDescription() == null, "Should have no description");
        helper.succeed();
    }

    @GameTest
    public void advancementsWithoutADisplayAreNotSent(GameTestHelper helper) {
        final var player = player(helper);

        // Recipe advancements have no display, so there's nothing to show
        final var recipe = holder(helper, "recipes/decorations/crafting_table");
        final var message = DiscordRelay.advancement(player, recipe, Component.literal("Unused"), EVENTS);

        helper.assertValueEqual(message, Optional.empty(), "message");
        helper.succeed();
    }

    @GameTest
    public void goingAfkAndComingBackAreSent(GameTestHelper helper) {
        final var player = player(helper);

        helper.assertValueEqual(line(DiscordRelay.afk(player, true, EVENTS)), name(player) + " is now AFK", "AFK");
        helper.assertValueEqual(
            line(DiscordRelay.afk(player, false, EVENTS)),
            name(player) + " is no longer AFK",
            "back"
        );
        helper.succeed();
    }

    @GameTest
    public void theServerStartingAndStoppingAreSentWithoutAFace(GameTestHelper helper) {
        final var started = DiscordRelay.serverStarted(EVENTS).orElseThrow().embeds().getFirst();
        final var stopped = DiscordRelay.serverStopped(EVENTS).orElseThrow().embeds().getFirst();

        helper.assertValueEqual(started.getAuthor().getName(), "🟢  The server has started", "started");
        helper.assertTrue(started.getAuthor().getIconUrl() == null, "Should have no face");
        helper.assertValueEqual(started.getColorRaw(), 0x55FF55, "started colour");
        helper.assertValueEqual(stopped.getAuthor().getName(), "🔴  The server has stopped", "stopped");
        helper.assertValueEqual(stopped.getColorRaw(), 0xFF5555, "stopped colour");
        helper.succeed();
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    private static String line(Optional<WebhookMessage> message) {
        return message.orElseThrow().embeds().getFirst().getAuthor().getName();
    }

    private static AdvancementHolder holder(GameTestHelper helper, String path) {
        return Objects.requireNonNull(
            helper.getLevel().getServer().getAdvancements().get(Identifier.withDefaultNamespace(path)),
            path
        );
    }

    /**
     * The advancement as it's sent, with the announcement the game would make for it.
     */
    private static Optional<MessageEmbed> advancement(
        GameTestHelper helper,
        ServerPlayer player,
        String path,
        EventsConfig events
    ) {
        final var holder = holder(helper, path);
        final var message = holder.value().display().orElseThrow().type().createAnnouncement(holder, player);
        return DiscordRelay.advancement(player, holder, message, events);
    }

    private static EventsConfig withJoin(EventsConfig.EventFormat join) {
        return new EventsConfig(
            join,
            EVENTS.quit(),
            EVENTS.death(),
            EVENTS.advancement(),
            EVENTS.afk(),
            EVENTS.afkReturn(),
            EVENTS.serverStart(),
            EVENTS.serverStop()
        );
    }

    private static EventsConfig withDescription(String descriptionFormat) {
        final var advancement = EVENTS.advancement();
        return new EventsConfig(
            EVENTS.join(),
            EVENTS.quit(),
            EVENTS.death(),
            new EventsConfig.AdvancementFormat(
                advancement.enabled(),
                advancement.format(),
                new DiscordTemplate(descriptionFormat),
                advancement.summaryFormat(),
                advancement.colour(),
                advancement.challengeColour()
            ),
            EVENTS.afk(),
            EVENTS.afkReturn(),
            EVENTS.serverStart(),
            EVENTS.serverStop()
        );
    }
}
