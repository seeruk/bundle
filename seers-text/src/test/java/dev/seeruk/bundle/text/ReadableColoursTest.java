package dev.seeruk.bundle.text;

import dev.seeruk.bundle.core.testing.TextChunks;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static dev.seeruk.bundle.core.testing.TextChunks.chunk;
import static dev.seeruk.bundle.text.ReadableColours.BOOK_PAGE;
import static dev.seeruk.bundle.text.ReadableColours.MIN_CONTRAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadableColoursTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static TextColor readable(int rgb) {
        return ReadableColours.readable(TextColor.fromRgb(rgb), BOOK_PAGE);
    }

    // The colour's red, green, and blue, each as a share of their total, which darkening keeps
    private static List<Long> shares(int rgb) {
        final var total = (double) (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF));
        return List.of(
            Math.round(((rgb >> 16) & 0xFF) / total * 50),
            Math.round(((rgb >> 8) & 0xFF) / total * 50),
            Math.round((rgb & 0xFF) / total * 50)
        );
    }

    @ParameterizedTest
    @EnumSource(value = ChatFormatting.class, names = {"BLACK", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA", "DARK_RED", "DARK_PURPLE", "DARK_GRAY", "BLUE", "RED"})
    void keepsVanillasDarkColours(ChatFormatting formatting) {
        final var colour = TextColor.fromLegacyFormat(formatting);

        assertEquals(colour, ReadableColours.readable(colour, BOOK_PAGE));
    }

    @ParameterizedTest
    @EnumSource(value = ChatFormatting.class, names = {"GOLD", "GRAY", "GREEN", "AQUA", "LIGHT_PURPLE", "YELLOW", "WHITE"})
    void darkensVanillasLightColoursUntilReadable(ChatFormatting formatting) {
        final var colour = ReadableColours.readable(TextColor.fromLegacyFormat(formatting), BOOK_PAGE);

        assertTrue(ReadableColours.contrast(colour.getValue(), BOOK_PAGE) >= MIN_CONTRAST, formatting + " isn't readable");
    }

    @Test
    void darkensOnlyAsMuchAsNeeded() {
        final var colour = readable(0x55FFFF).getValue();

        assertTrue(ReadableColours.contrast(colour, BOOK_PAGE) < MIN_CONTRAST + 0.1, "Darkened too much");
    }

    @Test
    void keepsTheHueOfColoursItDarkens() {
        final var colour = 0xFF8844;

        assertEquals(shares(colour), shares(readable(colour).getValue()));
    }

    @Test
    void darkensEachColourInStyledText() {
        // e.g. a nickname's gradient, where each character has its own colour
        final var text = Component.empty()
            .append(Component.literal("A").withStyle(Style.EMPTY.withColor(0xFFFFFF)))
            .append(Component.literal("B").withStyle(Style.EMPTY.withColor(0x55FFFF)));

        assertEquals(
            List.of(
                chunk("A", Style.EMPTY.withColor(readable(0xFFFFFF))),
                chunk("B", Style.EMPTY.withColor(readable(0x55FFFF)))
            ),
            TextChunks.chunks(ReadableColours.onLight(BOOK_PAGE, text))
        );
    }

    @Test
    void keepsTheRestOfTheStyle() {
        final var style = Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(true).withClickEvent(new ClickEvent.ChangePage(2));
        final var text = Component.literal("Hi").withStyle(style);

        assertEquals(
            List.of(chunk("Hi", style.withColor(readable(0xFFFFFF)))),
            TextChunks.chunks(ReadableColours.onLight(BOOK_PAGE, text))
        );
    }

    @Test
    void leavesTextWithoutAColourAsItIs() {
        final var text = Component.literal("Plain").withStyle(Style.EMPTY.withItalic(true));

        assertEquals(TextChunks.chunks(text), TextChunks.chunks(ReadableColours.onLight(BOOK_PAGE, text)));
    }
}
