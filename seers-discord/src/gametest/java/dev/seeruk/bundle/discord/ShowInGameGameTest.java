package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.discord.markdown.Mentions;
import dev.seeruk.bundle.text.TextTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.discord.DiscordFixtures.gameMessagesContaining;

public class ShowInGameGameTest {
    private static final DiscordConfig.GameFormats FORMATS = new DiscordConfig.GameFormats(
        TextTemplate.parse("[Discord] ${display_name}: ${message}"),
        TextTemplate.parse("")
    );

    @GameTest
    public void discordMessagesAreShownToEveryone(GameTestHelper helper) {
        // Unique, so only this test's message is found
        final var text = "Hello " + UUID.randomUUID();

        SeersDiscord.broadcast(helper.getLevel().getServer(), message("Seer", text), FORMATS);

        helper.assertValueEqual(
            gameMessagesContaining(text).stream().map(shown -> shown.getString()).toList(),
            List.of("[Discord] Seer: " + text),
            "messages shown"
        );
        helper.succeed();
    }

    @GameTest
    public void messagesWithNothingToShowAreNotShown(GameTestHelper helper) {
        final var name = "Seer " + UUID.randomUUID();

        SeersDiscord.broadcast(helper.getLevel().getServer(), message(name, ""), FORMATS);

        helper.assertValueEqual(gameMessagesContaining(name), List.of(), "messages shown");
        helper.succeed();
    }

    private static DiscordMessage message(String name, String content) {
        return new DiscordMessage(name, "seeruk", content, Mentions.NONE, Optional.empty(), List.of(), List.of());
    }
}
