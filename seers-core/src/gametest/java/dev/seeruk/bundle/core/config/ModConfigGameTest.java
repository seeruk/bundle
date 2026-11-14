package dev.seeruk.bundle.core.config;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

import java.nio.file.Files;

public class ModConfigGameTest {
    private static final String MOD_ID = "seers-core-gametest";
    private static final Codec<String> CODEC = Codec.STRING.fieldOf("greeting").codec();

    @GameTest
    public void loadsTheModsDistFile(GameTestHelper helper) {
        helper.assertValueEqual(ModConfig.load(MOD_ID, CODEC), "Hello from the dist file", "greeting");
        helper.assertTrue(
            Files.exists(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve(ModConfig.DIST_FILE)),
            "Dist file wasn't written to the config folder"
        );
        helper.succeed();
    }

    @GameTest
    public void refusesAModWithoutADistFile(GameTestHelper helper) {
        try {
            ModConfig.load("seers-core", CODEC);
            helper.fail("Loaded config for a mod without a dist file");
        } catch (ConfigException e) {
            helper.succeed();
        }
    }
}
