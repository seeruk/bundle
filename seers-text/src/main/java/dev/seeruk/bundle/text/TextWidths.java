package dev.seeruk.bundle.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Optional;

/**
 * How wide text is in pixels in Vanilla's default font, worked out on the server, so text can be
 * fitted to something with a fixed width, like a book page. Widths include the pixel of space after
 * each character. Characters outside printable ASCII use a wide default, so text measured with them
 * comes out a little too wide rather than too narrow.
 */
public final class TextWidths {
    /** How wide a character outside printable ASCII is taken to be. */
    public static final int DEFAULT_WIDTH = 9;

    // Each printable ASCII character's width, from ' ' (32) to '~' (126), as in the default font
    private static final int[] ASCII = {
        //   !  "  #  $  %  &  '  (  )  *  +  ,  -  .  /
        4, 2, 4, 6, 6, 6, 6, 2, 4, 4, 4, 6, 2, 6, 2, 6,
        // 0-9
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        // :  ;  <  =  >  ?  @
        2, 2, 5, 6, 5, 6, 7,
        // A-Z, where I is narrower
        6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        // [  \  ]  ^  _  `
        4, 6, 4, 6, 6, 3,
        // a-z, where f, i, k, l, and t are narrower
        6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6,
        // {  |  }  ~
        4, 2, 4, 7
    };

    private TextWidths() {}

    /**
     * How wide the text is, in pixels.
     */
    public static int width(String text, boolean bold) {
        final var characters = BreakIterator.getCharacterInstance(Locale.ROOT);
        characters.setText(text);

        var width = 0;
        var start = characters.first();
        for (var end = characters.next(); end != BreakIterator.DONE; start = end, end = characters.next()) {
            width += characterWidth(text.substring(start, end), bold);
        }
        return width;
    }

    /**
     * How wide the styled text is, in pixels, with bold parts measured as bold.
     */
    public static int width(Component text) {
        final var width = new int[1];
        text.visit(
            (style, content) -> {
                width[0] += width(content, style.isBold());
                return Optional.empty();
            }, Style.EMPTY
        );
        return width[0];
    }

    /**
     * How wide one character is, in pixels, including a character made of several (e.g. an emoji).
     */
    static int characterWidth(String character, boolean bold) {
        final var ascii = character.length() == 1 && character.charAt(0) >= ' ' && character.charAt(0) <= '~';
        return (ascii ? ASCII[character.charAt(0) - ' '] : DEFAULT_WIDTH) + (bold ? 1 : 0);
    }
}
