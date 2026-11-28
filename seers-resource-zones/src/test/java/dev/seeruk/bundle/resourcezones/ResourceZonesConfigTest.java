package dev.seeruk.bundle.resourcezones;

import dev.seeruk.bundle.core.config.DistConfigs;
import dev.seeruk.bundle.core.config.ModConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ResourceZonesConfigTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void readsTheShippedConfig() {
        assertDoesNotThrow(() -> DistConfigs.read(SeersResourceZones.MOD_ID, ModConfig.CONFIG, ResourceZonesConfig.CODEC));
    }
}
