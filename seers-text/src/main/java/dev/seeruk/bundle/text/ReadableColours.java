package dev.seeruk.bundle.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

/**
 * Text's colours darkened where they'd be hard to read on a light background, like a book's page,
 * as chat colours are picked to be read on a dark one. Colours are only darkened as much as they
 * need to be, keeping their hue, so e.g. a nickname's gradient still looks like itself.
 */
public final class ReadableColours {
    /** The colour of a book's page. */
    public static final int BOOK_PAGE = 0xFFFAEE;

    // How much a colour has to stand out from the background (as a WCAG contrast ratio). Vanilla's
    // dark aqua stands out the least of its dark colours on a book's page, just over this, so
    // Vanilla's dark colours are kept.
    static final double MIN_CONTRAST = 2.7;

    private ReadableColours() {}

    /**
     * The text with each colour too light for the background darkened, keeping everything else
     * about its style. Text without a colour is left as it is. The background has to be light, as
     * colours are only ever darkened.
     */
    public static Component onLight(int background, Component text) {
        final var readable = Component.empty();
        text.visit(
            (style, content) -> {
                if (!content.isEmpty()) {
                    readable.append(Component.literal(content).withStyle(readable(style, background)));
                }
                return Optional.empty();
            }, Style.EMPTY
        );
        return readable;
    }

    private static Style readable(Style style, int background) {
        final var colour = style.getColor();
        return colour == null ? style : style.withColor(readable(colour, background));
    }

    /**
     * The colour, or the lightest darker version of it that stands out enough from the background.
     */
    static TextColor readable(TextColor colour, int background) {
        if (contrast(colour.getValue(), background) >= MIN_CONTRAST) {
            return colour;
        }

        // Each step darkens it by a little more, stopping at the first that's readable, and black
        // always is on a light background
        for (var step = 1; step <= 100; step++) {
            final var darker = scale(colour.getValue(), 1 - step / 100.0);
            if (contrast(darker, background) >= MIN_CONTRAST) {
                return TextColor.fromRgb(darker);
            }
        }
        return TextColor.fromRgb(0);
    }

    /**
     * How much the two colours stand out from each other, as a WCAG contrast ratio, from 1 (the
     * same) to 21 (black and white).
     */
    static double contrast(int first, int second) {
        final var lighter = Math.max(luminance(first), luminance(second));
        final var darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    // How bright the colour looks, from 0 (black) to 1 (white), as WCAG defines it
    private static double luminance(int rgb) {
        return 0.2126 * linear(rgb >> 16) + 0.7152 * linear(rgb >> 8) + 0.0722 * linear(rgb);
    }

    private static double linear(int channel) {
        final var value = (channel & 0xFF) / 255.0;
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    // The colour with each of its red, green, and blue multiplied by the factor, keeping its hue
    private static int scale(int rgb, double factor) {
        final var red = (int) Math.round(((rgb >> 16) & 0xFF) * factor);
        final var green = (int) Math.round(((rgb >> 8) & 0xFF) * factor);
        final var blue = (int) Math.round((rgb & 0xFF) * factor);
        return (red << 16) | (green << 8) | blue;
    }
}
