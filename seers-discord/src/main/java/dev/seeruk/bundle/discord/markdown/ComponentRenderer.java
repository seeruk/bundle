package dev.seeruk.bundle.discord.markdown;

import dev.seeruk.bundle.text.TextColours;
import dev.vankka.simpleast.core.ParseException;
import dev.vankka.simpleast.core.TextStyle;
import dev.vankka.simpleast.core.node.Node;
import dev.vankka.simpleast.core.node.StyleNode;
import dev.vankka.simpleast.core.node.TextNode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Turns Discord's Markdown into styled text for the game, as well as the game can show it...
 * Anything else is shown as it was written. Tags that could be parsed in-game (e.g. {@code <red>})
 * are not parsed here, and are also shown as they were written.
 *
 * <p>The message is parsed into a tree of nodes first (see {@link MarkdownParser}), which is
 * then translated into a {@link Component} tree.
 */
public final class ComponentRenderer {
    // Starts one of the game's old formatting codes, which it still reads in any text
    private static final String SECTION_SIGN = "\u00A7";

    private ComponentRenderer() {}

    /**
     * Returns the given markdown as styled text for Minecraft. If it can't be parsed for whatever
     * reason, it's shown as it was written.
     *
     * @param markdown The message as it was written in Discord.
     * @param mentions The names of what the message mentions.
     */
    public static Component render(String markdown, Mentions mentions) {
        try {
            return render(MarkdownParser.parse(markdown), mentions);
        } catch (ParseException e) {
            return literal(markdown);
        }
    }

    /**
     * Returns the nodes as one Component. SimpleAST splits plain text up into many small nodes
     * (e.g. at each punctuation mark), so neighbouring text nodes are joined back together first.
     */
    private static MutableComponent render(List<Node<Object>> nodes, Mentions mentions) {
        final var result = Component.empty();
        final var text = new StringBuilder();

        for (final var node : nodes) {
            if (node instanceof TextNode<Object> textNode) {
                text.append(textNode.getContent());
                continue;
            }

            // The text before this node goes first, to keep everything in order. The builder is
            // then emptied, ready for the next run of text.
            if (!text.isEmpty()) {
                result.append(literal(text.toString()));
                text.setLength(0);
            }

            result.append(render(node, mentions));
        }

        return text.isEmpty() ? result : result.append(literal(text.toString()));
    }

    /**
     * Returns the node as a Component, i.e. its children, with the node's own styles applied to
     * them. Styles from further down the tree (e.g. italics inside bold) are already applied to
     * the children, and are kept.
     */
    private static MutableComponent render(Node<Object> node, Mentions mentions) {
        var result = node.hasChildren() ? render(node.getChildren(), mentions) : Component.empty();
        if (node instanceof StyleNode<?, ?> styled) {
            for (final var style : styled.getStyles()) {
                result = style(result, style, mentions);
            }
        }
        return result;
    }

    /**
     * Returns the node's content with the style applied, which can also change what's shown, e.g.
     * a spoiler is shown as "[spoiler]", with its content hidden in a hover.
     */
    private static MutableComponent style(MutableComponent content, Object style, Mentions mentions) {
        return switch (style) {
            case TextStyle text -> switch (text.getType()) {
                case BOLD -> content.withStyle(ChatFormatting.BOLD);
                case ITALICS -> content.withStyle(ChatFormatting.ITALIC);
                case UNDERLINE -> content.withStyle(ChatFormatting.UNDERLINE);
                case STRIKETHROUGH -> content.withStyle(ChatFormatting.STRIKETHROUGH);
                default -> content;
            };
            case MarkdownElement.CodeBlock _, MarkdownElement.Code _ -> content.withStyle(ChatFormatting.GRAY);
            case MarkdownElement.Quote _, MarkdownElement.SmallText _ -> content.withStyle(ChatFormatting.GRAY);
            case MarkdownElement.Heading _ -> content.withStyle(ChatFormatting.BOLD);
            // Spoilers don't use TextColours.HOVERABLE because that'd stand out a lot for a
            // spoiler, we just use dark grey to keep it subtle.
            case MarkdownElement.Spoiler _ -> Component.literal("[spoiler]").withStyle(spoiler -> spoiler
                .withColor(ChatFormatting.DARK_GRAY)
                .withHoverEvent(new HoverEvent.ShowText(content)));
            case MarkdownElement.Link link -> linked(content, link.url());
            case MarkdownElement.Mention mention -> mentioned(mention, mentions);
            default -> content;
        };
    }

    /**
     * Returns plain text from Discord as a Component, as it should be shown in game. Any emoji the
     * game can't show are swapped for their names (see {@link Emojis#named}), and section signs
     * are removed, as the game would otherwise read them as formatting codes (e.g. "§c" for red).
     */
    public static MutableComponent literal(String text) {
        return Component.literal(Emojis.named(text.replace(SECTION_SIGN, "")));
    }

    /**
     * Returns the label as a link to the URL, which opens it when clicked, and shows it when
     * hovered over. It's underlined, as links usually are, and coloured as something clickable (see
     * {@link TextColours#CLICKABLE}). If the URL isn't a valid one, there's no link, and the label
     * is left as it is.
     */
    public static MutableComponent linked(MutableComponent label, String url) {
        try {
            final var uri = new URI(url);
            return label.withStyle(style -> style
                .withColor(TextColours.CLICKABLE)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(uri))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(url))));
        } catch (URISyntaxException e) {
            return label;
        }
    }

    /**
     * Returns a mention by its name, e.g. {@code @Seer}, in green so it stands out. If its ID isn't
     * one of the message's mentions, it's shown as unknown, as Discord does.
     */
    private static MutableComponent mentioned(MarkdownElement.Mention mention, Mentions mentions) {
        final var name = switch (mention.kind()) {
            case USER -> "@" + name(mentions.users(), mention.id(), "unknown-user");
            case ROLE -> "@" + name(mentions.roles(), mention.id(), "unknown-role");
            case CHANNEL -> "#" + name(mentions.channels(), mention.id(), "unknown");
        };
        return literal(name).withStyle(ChatFormatting.GREEN);
    }

    private static String name(Map<Long, String> names, String id, String unknown) {
        // Anyone can write an ID too long to be a real one
        return id.length() <= 18 ? names.getOrDefault(Long.parseLong(id), unknown) : unknown;
    }
}
