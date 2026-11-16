package dev.seeruk.bundle.text;

import com.mojang.serialization.Codec;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * Trusted text with named placeholders like {@code ${player}}, which are filled in each time it's
 * used. It's parsed once, so filling it in doesn't need to parse anything.
 */
public final class TextTemplate {
    public static final Codec<TextTemplate> CODEC = Codec.STRING.xmap(TextTemplate::parse, TextTemplate::template);

    private static final ParserContext.Key<Function<String, Component>> VALUES =
        ParserContext.Key.of("seers_text:template_values");

    // The trusted profile (see TextParsers#TRUSTED), with placeholders filled in from VALUES
    private static final NodeParser PARSER = ParserBuilder.of()
        .placeholders(TagLikeParser.PLACEHOLDER_USER, VALUES)
        .customTagRegistry(TextParsers.TRUSTED_TAGS)
        .quickText()
        .markdown()
        .build();

    private final String template;
    private final TextNode node;

    private TextTemplate(String template, TextNode node) {
        this.template = template;
        this.node = node;
    }

    public static TextTemplate parse(String template) {
        return new TextTemplate(template, PARSER.parseNode(template));
    }

    /**
     * The template as it was written.
     */
    public String template() {
        return template;
    }

    /**
     * Fills in the placeholders, e.g. {@code Map.of("player", name)} for {@code ${player}}. Any
     * placeholder without a value is left as it was written, so mistakes are easy to spot.
     */
    public Component format(Map<String, Component> values) {
        return node.toComponent(ParserContext.of(
            VALUES, name ->
                values.getOrDefault(name, Component.literal("${" + name + "}"))
        ));
    }
}
