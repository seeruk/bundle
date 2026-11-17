package dev.seeruk.bundle.discord;

import com.mojang.serialization.Codec;
import dev.seeruk.bundle.text.TextParsers;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.DynamicTextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A template for text shown in game, with placeholders in the format {@code ${placeholder}}. It
 * can use every formatting tag (i.e. it is "trusted"), as it's written by the server's admins.
 *
 * @param template The unformatted template.
 */
public record GameTemplate(String template) {
    public static final Codec<GameTemplate> CODEC = Codec.STRING.xmap(GameTemplate::new, GameTemplate::template);

    private static final ParserContext.Key<Function<String, Component>> KEY = DynamicTextNode.key(
        SeersDiscord.id("game_template").toString()
    );

    // These are the only placeholders, so anything else written like one is left as it was written
    private static final Set<String> PLACEHOLDERS = Set.of("reply", "display_name", "username", "message");

    private static final NodeParser PARSER = TextParsers.trusted()
        .placeholders(TagLikeParser.PLACEHOLDER_USER, PLACEHOLDERS, KEY)
        .build();

    /**
     * Fills in the given placeholders (e.g. {@code Map.of("message", text)} for
     * {@code ${message}}). Values are components, so they're shown as they are. Their contents are
     * never parsed further (so they can contain tags without rendering them unsafely).
     */
    public Component format(Map<String, Component> values) {
        return PARSER.parseNode(template).toComponent(ParserContext.of(
            KEY,
            name -> values.getOrDefault(name, Component.literal("${" + name + "}"))
        ));
    }
}
