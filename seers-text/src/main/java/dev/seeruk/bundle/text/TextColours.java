package dev.seeruk.bundle.text;

import net.minecraft.ChatFormatting;

/**
 * Colours for text that can be interacted with, which the mods put into messages (e.g. a link),
 * so the same colour means the same thing everywhere.
 */
public final class TextColours {
    /**
     * For text that shows more when hovered over, e.g. the rest of a message that's been cut down.
     * Vanilla rarely uses it for plain text, and where it does (e.g. epic items), the text shows
     * more when hovered over too.
     */
    public static final ChatFormatting HOVERABLE = ChatFormatting.LIGHT_PURPLE;

    /**
     * For text that does something when clicked, e.g. opening a link, or copying something. It may
     * show more when hovered over too, but clicking is the main thing it's for.
     */
    public static final ChatFormatting CLICKABLE = ChatFormatting.AQUA;

    private TextColours() {}
}
