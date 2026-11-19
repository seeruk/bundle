package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.core.config.ModConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class DiscordConfigGameTest {
    @GameTest
    public void distConfigIsValid(GameTestHelper helper) {
        // Nothing's set by default, so Discord stays off until an admin sets it up
        final var config = ModConfig.load(SeersDiscord.MOD_ID, DiscordConfig.CODEC);

        helper.assertValueEqual(config.botToken(), "", "bot token");
        helper.assertValueEqual(config.channelId(), "", "channel ID");
        helper.assertValueEqual(config.discord().chat().template(), "${message}", "chat format");
        helper.assertValueEqual(
            config.discord().emote().template(),
            "_${display_name} ${message}_",
            "emote format"
        );
        helper.assertValueEqual(
            config.game().chat().template(),
            "${reply}<blue><<hover 'Discord user @${username}'>${display_name}</hover>></blue> ${message}",
            "game chat format"
        );
        helper.assertValueEqual(
            config.game().reply().template(),
            "<gray><hover '${display_name}: ${message}'>➥</hover></gray> ",
            "game reply format"
        );
        helper.assertValueEqual(config.discord().commands().publicReplies(), false, "public replies");
        helper.assertTrue(config.discord().commands().players().enabled(), "/players should be on");
        helper.assertTrue(config.discord().commands().status().enabled(), "/status should be on");
        helper.succeed();
    }
}
