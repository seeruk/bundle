package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.core.config.ModConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class AfkConfigGameTest {
    @GameTest
    public void distConfigIsValid(GameTestHelper helper) {
        final var config = ModConfig.load(SeersAFK.MOD_ID, AfkConfig.CODEC);

        helper.assertValueEqual(config.timeoutSeconds(), 300, "timeout");
        helper.assertValueEqual(config.messages().afk().template(), "<yellow>${player} is now AFK</yellow>", "AFK message");
        helper.assertValueEqual(config.messages().back().template(), "<yellow>${player} is no longer AFK</yellow>", "back message");
        helper.succeed();
    }
}
