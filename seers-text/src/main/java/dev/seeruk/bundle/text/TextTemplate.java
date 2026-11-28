package dev.seeruk.bundle.text;

import com.mojang.serialization.Codec;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

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

    // A placeholder's name is everything between its ${ and }
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");

    private final String template;
    private final TextNode node;
    private final List<String> placeholders;

    private TextTemplate(String template, TextNode node, List<String> placeholders) {
        this.template = template;
        this.node = node;
        this.placeholders = placeholders;
    }

    public static TextTemplate parse(String template) {
        final var placeholders = PLACEHOLDER.matcher(template).results().map(match -> match.group(1)).toList();
        return new TextTemplate(template, PARSER.parseNode(template), placeholders);
    }

    /**
     * The template as it was written.
     */
    public String template() {
        return template;
    }

    /**
     * The names of the template's placeholders, in the order they're written, e.g.
     * {@code player} for {@code ${player}}.
     */
    public List<String> placeholders() {
        return placeholders;
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
