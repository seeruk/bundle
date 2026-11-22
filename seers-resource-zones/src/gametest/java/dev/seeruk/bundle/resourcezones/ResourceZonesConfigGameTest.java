package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.config.ModConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.TextColor;

public class ResourceZonesConfigGameTest {
    @GameTest
    public void distConfigIsValid(GameTestHelper helper) {
        final var config = ModConfig.load(SeersResourceZones.MOD_ID, ResourceZonesConfig.CODEC);

        helper.assertValueEqual(
            config.particles().colour(),
            TextColor.fromLegacyFormat(ChatFormatting.GOLD),
            "particle colour"
        );
        helper.assertValueEqual(config.particles().radius(), 16, "particle radius");
        helper.assertValueEqual(config.export().folder(), "resource-zones", "export folder");
        helper.succeed();
    }
}
