package dev.seeruk.bundle.resourcezones.particles;

import com.mojang.serialization.JavaOps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParticleSettingsTest {
    static Stream<Arguments> shouldSaveAndLoad() {
        return Stream.of(
            Arguments.of("Saves nothing for the defaults", ParticleSettings.DEFAULTS, Map.of()),
            Arguments.of(
                "Saves only what isn't a default",
                ParticleSettings.DEFAULTS.withStyle(ParticleStyle.WALL),
                Map.of("style", "wall")
            ),
            Arguments.of(
                "Saves everything that isn't a default",
                ParticleSettings.DEFAULTS.withIntensity(ParticleIntensity.HIGH).withStyle(ParticleStyle.WALL),
                Map.of("intensity", "high", "style", "wall")
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldSaveAndLoad(final String description, final ParticleSettings settings, final Map<String, String> saved) {
        assertEquals(saved, ParticleSettings.CODEC.encodeStart(JavaOps.INSTANCE, settings).getOrThrow());
        assertEquals(settings, ParticleSettings.CODEC.parse(JavaOps.INSTANCE, saved).getOrThrow());
    }
}
