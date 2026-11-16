package dev.seeruk.bundle.discord;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Utilities for working with Discord Markdown, either from text or Minecraft components.
 */
public final class DiscordMarkdown {
    // Characters Discord's Markdown can treat specially. Discord shows any punctuation with a
    // backslash before it as it is.
    private static final Pattern SPECIAL = Pattern.compile("[\\\\*_~`|<>#\\-\\[\\]]");

    private static final Pattern LINK = Pattern.compile("https?://\\S+");

    // Keeps markers from neighbouring runs of text apart, so for example, bold then italic isn't
    // read as one run of asterisks...
    private static final String SEPARATOR = "\u200B"; // It's a zero-width space

    private DiscordMarkdown() {}

    /**
     * Escapes the text, so Discord shows it as it is, rather than as Markdown. Links are left as
     * they are, so Discord still links them.
     */
    public static String escape(String text) {
        final var escaped = new StringBuilder();
        final var links = LINK.matcher(text);
        var end = 0;

        while (links.find()) {
            escaped.append(escapeAll(text.substring(end, links.start()))).append(links.group());
            end = links.end();
        }

        return escaped.append(escapeAll(text.substring(end))).toString();
    }

    private static String escapeAll(String text) {
        return SPECIAL.matcher(text).replaceAll("\\\\$0");
    }

    /**
     * The given component returned as a string with styles we can convert to Discord's Markdown
     * converted, and everything else escaped. Discord can't show colours, so they're left out.
     */
    public static String of(Component text) {
        final var markdown = new StringBuilder();
        var markedBefore = false;

        for (final var run : runs(text)) {
            final var markers = run.markers();
            final var core = run.text().strip();
            if (markers.isEmpty() || core.isEmpty()) {
                markdown.append(escape(run.text()));
                markedBefore = false;
                continue;
            }

            // Markers have to be right next to the text they mark, so spaces go outside them
            final var start = run.text().indexOf(core);
            final var leading = run.text().substring(0, start);
            if (markedBefore && leading.isEmpty()) {
                markdown.append(SEPARATOR);
            }

            markdown.append(leading)
                .append(markers)
                .append(escape(core))
                .append(new StringBuilder(markers).reverse())
                .append(run.text().substring(start + core.length()));
            markedBefore = run.text().endsWith(core);
        }

        return markdown.toString();
    }

    /**
     * A run of text that has the same Markdown style markers all the way through.
     */
    private record Run(String text, String markers) {}

    /**
     * The text, split into runs with the same Markdown style, neighbouring pieces with the same
     * Markdown styles are joined together.
     */
    private static List<Run> runs(Component text) {
        final var runs = new ArrayList<Run>();
        text.visit(
            (style, content) -> {
                if (!content.isEmpty()) {
                    final var markers = markers(style);
                    if (!runs.isEmpty() && runs.getLast().markers().equals(markers)) {
                        // If the markers are the same as the last run, join them together. We do
                        // this by removing it, and appending the new content to it and adding the
                        // result.
                        runs.add(new Run(runs.removeLast().text() + content, markers));
                    } else {
                        runs.add(new Run(content, markers));
                    }
                }
                return Optional.empty();
            }, Style.EMPTY
        );
        return runs;
    }

    /**
     * The corresponding Markdown for styles that we can translate to Discord Markdown.
     */
    private static String markers(Style style) {
        return (style.isBold() ? "**" : "")
            + (style.isItalic() ? "*" : "")
            + (style.isUnderlined() ? "__" : "")
            + (style.isStrikethrough() ? "~~" : "");
    }
}
