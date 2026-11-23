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
            config.notice().template(),
            "<gold>Resource zone</gold> <gray>- this area is reset regularly</gray>",
            "notice"
        );

        helper.assertValueEqual(
            config.particles().colour(),
            TextColor.fromLegacyFormat(ChatFormatting.GOLD),
            "particle colour"
        );
        helper.assertValueEqual(config.particles().radiusBlocks(), 16, "particle radius");
        helper.assertValueEqual(
            config.edit().status().template(),
            "<gold>Editing resource zones</gold> <gray>- ${mode}</gray> <green>+${added}</green> <red>-${removed}</red>",
            "edit status"
        );
        helper.assertValueEqual(config.edit().markers().radiusChunks(), 8, "marker radius");
        helper.assertValueEqual(
            config.edit().markers().colours(),
            new ResourceZonesConfig.MarkerColours(
                TextColor.fromLegacyFormat(ChatFormatting.BLUE),
                TextColor.fromLegacyFormat(ChatFormatting.GREEN),
                TextColor.fromLegacyFormat(ChatFormatting.RED)
            ),
            "marker colours"
        );
        helper.assertValueEqual(config.export().folder(), "resource-zones", "export folder");
        helper.succeed();
    }
}
