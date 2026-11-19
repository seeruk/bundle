package dev.seeruk.bundle.core.tick;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class TickStatsGameTest {
    @GameTest
    public void averagesAreMeasuredFromTheServersTicks(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            for (final var window : TickStats.Window.values()) {
                final var tps = TickStats.ticksPerSecond(window);
                final var mspt = TickStats.millisecondsPerTick(window);

                // The GameTest server ticks as fast as it can, so this only checks they're real
                helper.assertTrue(tps > 0 && Double.isFinite(tps), "TPS should be measured: " + tps);
                helper.assertTrue(mspt >= 0 && Double.isFinite(mspt), "MSPT should be measured: " + mspt);
            }
        });
    }
}
