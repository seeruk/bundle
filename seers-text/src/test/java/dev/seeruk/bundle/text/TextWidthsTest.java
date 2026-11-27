package dev.seeruk.bundle.text;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextWidthsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    static Stream<Arguments> shouldMeasureText() {
        return Stream.of(
            Arguments.of("Measures most letters as 6", "aW", false, 12),
            Arguments.of("Measures narrow characters as narrower", "il!", false, 7),
            Arguments.of("Measures a space as 4", " ", false, 4),
            Arguments.of("Adds 1 to each bold character", "ab", true, 14),
            Arguments.of("Measures characters outside ASCII as the default", "é😀", false, 18),
            Arguments.of("Measures an emoji made of others as one character", "👨‍👩", false, 9),
            Arguments.of("Measures nothing as 0", "", false, 0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldMeasureText(final String description, final String text, final boolean bold, final int expected) {
        assertEquals(expected, TextWidths.width(text, bold));
    }

    static Stream<Arguments> shouldMeasureStyledText() {
        return Stream.of(
            Arguments.of("Measures plain text", Component.literal("ab"), 12),
            Arguments.of(
                "Measures bold parts as bold",
                Component.literal("a").append(Component.literal("b").withStyle(Style.EMPTY.withBold(true))),
                13
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldMeasureStyledText(final String description, final Component text, final int expected) {
        assertEquals(expected, TextWidths.width(text));
    }

    // Vanilla measures each character in its font image by its rightmost pixel that isn't
    // transparent, then adds a pixel of space after it. The image is a 16 by 16 grid of characters,
    // in character code order. Spaces aren't drawn, and are given their width in the font's config.
    @Test
    void matchesVanillasDefaultFont() throws IOException {
        final BufferedImage font;
        try (final var stream = TextWidthsTest.class.getResourceAsStream("/assets/minecraft/textures/font/ascii.png")) {
            font = ImageIO.read(Objects.requireNonNull(stream, "Vanilla's font isn't on the classpath"));
        }
        final var cell = font.getWidth() / 16;

        for (var character = '!'; character <= '~'; character++) {
            var drawn = 0;
            for (var x = 0; x < cell; x++) {
                for (var y = 0; y < cell; y++) {
                    if (font.getRGB((character % 16) * cell + x, (character / 16) * cell + y) >>> 24 != 0) {
                        drawn = x + 1;
                    }
                }
            }
            assertEquals(drawn + 1, TextWidths.width(String.valueOf(character), false), "Width of " + character);
        }
    }
}
