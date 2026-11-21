package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.text.TextComponents;

import java.util.regex.Pattern;

/**
 * Utilities to check rules about Discord Webhook names.
 */
public final class WebhookNames {
    public static final int MAX_LENGTH = 80;

    // Discord refuses names with these words in them
    private static final Pattern RESERVED_WORD = Pattern.compile("(?i)discord|clyde");

    private WebhookNames() {}

    public static boolean hasReservedWord(String name) {
        return RESERVED_WORD.matcher(name).find();
    }

    /**
     * The name as Discord will allow it. Reserved words are visibly censored (e.g.
     * {@code D*****d}), so it's clear the name's been changed, and long names are cut down.
     */
    public static String allowed(String name) {
        final var censored = RESERVED_WORD.matcher(name).replaceAll(word -> {
            final var text = word.group();
            return text.charAt(0) + "*".repeat(text.length() - 2) + text.charAt(text.length() - 1);
        });

        return TextComponents.truncate(censored, MAX_LENGTH);
    }
}
