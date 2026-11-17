package dev.seeruk.bundle.discord.markdown;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.EmojiType;

import java.util.EnumSet;

/**
 * Utilities for showing emojis from Discord in game.
 */
public final class Emojis {
    // Emojis are sometimes represented as a Unicode symbol and an invisible character, this is the
    // variation selector, Minecraft can't show these coloured symbols, but can show the basic
    // Unicode versions, so we strip this off.
    private static final String EMOJI_VARIATION_SELECTOR = "\uFE0F";

    private Emojis() {}

    /**
     * Takes the input string and replaces all emojis within the text with either their basic
     * Unicode version (for those supported by Minecraft's font), or their Discord name otherwise,
     * like {@code :smile:}.
     */
    public static String named(String text) {
        return EmojiManager.replaceAllEmojis(text, Emojis::name, EnumSet.of(EmojiType.UNICODE));
    }

    private static String name(Emoji emoji) {
        final var text = emoji.getEmoji();

        // Minecraft's font only covers the first 65,536 characters, which other emoji are past
        if (text.codePoints().allMatch(Character::isBmpCodePoint)) {
            return text.replace(EMOJI_VARIATION_SELECTOR, "");
        }

        // The first is the one Discord shows, e.g. :smile: rather than :D
        return emoji.getDiscordAliases().stream().findFirst().orElse(text);
    }
}
