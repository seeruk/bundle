package dev.seeruk.bundle.text;

import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1.MarkdownFormat;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import net.minecraft.network.chat.Component;

/**
 * Contains parser profiles available for use in various contexts based on whether the source is
 * trusted or not.
 */
public final class TextParsers {
    // Tags for trusted use-cases, for example, text written by the server, taken from config, or
    // written by very trusted players (e.g. operators).
    static final TagRegistry TRUSTED_TAGS = TagRegistry.builderCopyDefault()
        .build();

    // The tags for untrusted use-cases, for example, player-written text. Currently, we rely mostly
    // on what Text Placeholder API considers safe, but we do remove a couple of tags that are not
    // safe for player-written text, including explicitly removing all click-type tags.
    private static final TagRegistry UNTRUSTED_TAGS = TagRegistry.builderCopySafe()
        .remove("change_page")
        .remove("click")
        .remove("copy_to_clipboard")
        .remove("hover")
        .remove("open_url")
        .remove("run_cmd")
        .remove("suggest_command")
        .build();

    // Only the safe tags that change the colour of text, e.g. named colours, hex colours, rainbows,
    // and gradients. Shadows are a separate effect, so they're left out. A little more complex to
    // build than some other tag sets.
    private static final TagRegistry COLOUR_TAGS = colourTags();

    /**
     * Supports basic markdown, colours, formatting tags, placeholders, and interactive tags like
     * click and hover. Intended to be used by trusted sources; for example, in mod configuration,
     * in mod code, by server operators, so on.
     */
    public static final NodeParser TRUSTED = ParserBuilder.of()
        .customTagRegistry(TRUSTED_TAGS)
        .quickText()
        .markdown() // TODO: Verify this is sane.
        .build();

    /**
     * Supports basic markdown, colours and formatting tags, but does not support things like
     * placeholders, clicks, hovers, line breaks, or anything particularly interactive.
     */
    public static final NodeParser UNTRUSTED = ParserBuilder.of()
        .customTagRegistry(UNTRUSTED_TAGS)
        .quickText()
        .markdown(MarkdownFormat.BOLD, MarkdownFormat.ITALIC, MarkdownFormat.UNDERLINE, MarkdownFormat.STRIKETHROUGH)
        .requireSafe()
        .build();

    /**
     * Supports colour tags only, without markdown or any other kind of formatting (e.g. bold or
     * italic). Intended for untrusted text that should only ever be coloured, like nicknames.
     */
    public static final NodeParser COLOURS = ParserBuilder.of()
        .customTagRegistry(COLOUR_TAGS)
        .quickText()
        .requireSafe()
        .build();

    private TextParsers() {}

    /**
     * Parses trusted text with the {@link #TRUSTED} profile.
     */
    public static Component parseAsTrusted(String text) {
        return TRUSTED.parseNode(text).toComponent();
    }

    /**
     * Parses untrusted text with the {@link #UNTRUSTED} profile.
     */
    public static Component parseAsUntrusted(String text) {
        return UNTRUSTED.parseNode(text).toComponent();
    }

    /**
     * Parses untrusted text with the {@link #COLOURS} profile.
     */
    public static Component parseAsColours(String text) {
        return COLOURS.parseNode(text).toComponent();
    }

    private static TagRegistry colourTags() {
        final var builder = TagRegistry.builder();
        TagRegistry.copySafe().getTags().stream()
            .filter(tag -> tag.type().equals("color") || tag.type().equals("gradient"))
            .filter(tag -> !tag.name().equals("shadow"))
            .forEach(builder::add);

        return builder.build();
    }
}
