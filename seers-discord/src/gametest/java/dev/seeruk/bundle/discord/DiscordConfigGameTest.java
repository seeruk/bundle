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
        helper.assertValueEqual(config.formats().chat().template(), "${message}", "chat format");
        helper.assertValueEqual(config.formats().emote().template(), "_${display_name} ${message}_", "emote format");
        helper.succeed();
    }
}
