package dev.seeruk.bundle.afk;

import dev.seeruk.bundle.core.config.DistConfigs;
import dev.seeruk.bundle.core.config.ModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AfkConfigTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void readsTheShippedConfig() {
        assertDoesNotThrow(() -> DistConfigs.read(SeersAfk.MOD_ID, ModConfig.CONFIG, AfkConfig.CODEC));
    }
}
