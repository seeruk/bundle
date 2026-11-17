package dev.seeruk.bundle.discord.markdown;

import dev.vankka.simpleast.core.ParseException;
import dev.vankka.simpleast.core.TextStyle;
import dev.vankka.simpleast.core.node.Node;
import dev.vankka.simpleast.core.node.StyleNode;
import dev.vankka.simpleast.core.node.TextNode;
import dev.vankka.simpleast.core.parser.ParseSpec;
import dev.vankka.simpleast.core.parser.Parser;
import dev.vankka.simpleast.core.parser.Rule;
import dev.vankka.simpleast.core.simple.SimpleMarkdownRules;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Discord's Markdown into a tree of nodes, with SimpleAST and a set of rules for Discord's
 * Markdown. SimpleAST tries each rule in order at each position in the text, and the first that
 * matches wins.
 *
 * <p>SimpleAST provides the basics (e.g. bold), and the rest are Discord's own (e.g. spoilers).
 * Nodes for the rest are {@link StyleNode}s with a {@link MarkdownElement}, saying what they are.
 *
 * <p>Language comes from SimpleAST itself, and other parser-related literature like
 * <a href="https://craftinginterpreters.com/representing-code.html">Crafting Interpreters</a>.
 */
final class MarkdownParser {
    // Patterns start with ^, as a rule has to match where the parser has got to in the text

    // e.g. ```java\ncode```, where the language is optional
    private static final Pattern CODE_BLOCK = Pattern.compile("^```(?:[\\w+#-]+\\n)?([\\s\\S]+?)\\n?```");
    // e.g. `code`, or ``code`` (which can have a ` inside it)
    private static final Pattern CODE = Pattern.compile("^(``?)([\\s\\S]+?)\\1(?!`)");

    private static final Pattern LINE_BREAK = Pattern.compile("^\\n");

    // ">>> " quotes everything after it, and "> " just the rest of its line
    private static final Pattern LONG_QUOTE = Pattern.compile("^>>> ([\\s\\S]*)");
    private static final Pattern QUOTE = Pattern.compile("^> ([^\\n]*)");
    // e.g. "# Title", down to "### Title"
    private static final Pattern HEADING = Pattern.compile("^#{1,3} ([^\\n]*)");
    private static final Pattern SMALL_TEXT = Pattern.compile("^-# ([^\\n]*)");
    // e.g. "- item" or "* item", keeping any indent before it
    private static final Pattern LIST_ITEM = Pattern.compile("^(\\s*)[-*] ([^\\n]*)");

    private static final Pattern SPOILER = Pattern.compile("^\\|\\|([\\s\\S]+?)\\|\\|");

    // Mentions are IDs, e.g. <@123>, with names looked up later. Users can also be <@!123>, which
    // older Discord apps sent for nicknames.
    private static final Pattern USER = Pattern.compile("^<@!?(\\d+)>");
    private static final Pattern ROLE = Pattern.compile("^<@&(\\d+)>");
    private static final Pattern CHANNEL = Pattern.compile("^<#(\\d+)>");
    // e.g. <:pepe:123>, or <a:pepe:123> for animated ones
    private static final Pattern CUSTOM_EMOJI = Pattern.compile("^<a?:(\\w+):\\d+>");

    // e.g. [text](https://example.com), where the URL can also be in angle brackets
    private static final Pattern MASKED_LINK = Pattern.compile("^\\[([^\\[\\]]+)]\\(<?(https?://[^\\s)>]+)>?\\)");
    // e.g. <https://example.com>, which stops Discord showing a preview of it
    private static final Pattern ANGLED_LINK = Pattern.compile("^<(https?://[^\\s>]+)>");
    // Punctuation at the end is more likely the sentence's than the link's
    private static final Pattern LINK = Pattern.compile("^https?://[^\\s<]*[^\\s<.,:;\"')\\]!?]");

    // SimpleAST's italics, except that italics with * can end with a space, as they can in Discord
    // (e.g. "*hi *"). Italics with _ have to end a word, so snake_case_ is only italic from "case",
    // and can't have a lone _ inside, so _snake_case isn't italic at all.
    private static final Pattern ITALICS = Pattern.compile(
        "^\\b_((?:__|\\\\[\\s\\S]|[^\\\\_])+?)_\\b|^\\*(?=\\S)((?:\\*\\*|\\\\[\\s\\S]|[^*\\\\])+?)\\*(?!\\*)"
    );

    private MarkdownParser() {}

    /**
     * Returns the message parsed into a tree of nodes.
     *
     * @throws ParseException If no rule matches part of the message, which the text rule (last)
     *                        should make impossible...
     */
    static List<Node<Object>> parse(String markdown) {
        return parser().parse(markdown);
    }

    /**
     * Returns a parser with the rules for Discord's Markdown. The rules each keep a
     * {@link Matcher}, which holds where it's got to in the text it's matching, so a parser can't
     * be shared between threads. A new one is made for each message, which is cheap, as the
     * patterns themselves are only compiled once.
     */
    private static Parser<Object, Node<Object>, Object> parser() {
        return new Parser<Object, Node<Object>, Object>().addRules(List.of(
            // Code goes first, as nothing inside it is parsed
            terminal(CODE_BLOCK, found -> styled(new MarkdownElement.CodeBlock(), found.group(1))),
            terminal(CODE, found -> styled(new MarkdownElement.Code(), found.group(2))),

            SimpleMarkdownRules.createEscapeRule(),

            // Each line break is kept, including blank lines, which SimpleAST's own rule merges
            terminal(LINE_BREAK, found -> new TextNode<>("\n")),

            lineStart(LONG_QUOTE, 1, found -> new StyleNode<>(List.of(new MarkdownElement.Quote()))),
            lineStart(QUOTE, 1, found -> new StyleNode<>(List.of(new MarkdownElement.Quote()))),
            lineStart(HEADING, 1, found -> new StyleNode<>(List.of(new MarkdownElement.Heading()))),
            lineStart(SMALL_TEXT, 1, found -> new StyleNode<>(List.of(new MarkdownElement.SmallText()))),
            // The bullet is the item's first text, so it's joined with the text after it
            lineStart(
                LIST_ITEM, 2, found -> {
                    final var item = new StyleNode<Object, MarkdownElement>(List.of());
                    item.addChild(new TextNode<>(found.group(1) + "• "));
                    return item;
                }
            ),

            nonterminal(SPOILER, 1, found -> new StyleNode<>(List.of(new MarkdownElement.Spoiler()))),

            terminal(USER, found -> mention(MarkdownElement.MentionKind.USER, found)),
            terminal(ROLE, found -> mention(MarkdownElement.MentionKind.ROLE, found)),
            terminal(CHANNEL, found -> mention(MarkdownElement.MentionKind.CHANNEL, found)),
            terminal(CUSTOM_EMOJI, found -> new TextNode<>(":" + found.group(1) + ":")),

            nonterminal(MASKED_LINK, 1, found -> new StyleNode<>(List.of(new MarkdownElement.Link(found.group(2))))),
            terminal(ANGLED_LINK, found -> link(found.group(1))),
            terminal(LINK, found -> link(found.group())),

            SimpleMarkdownRules.createBoldRule(),
            SimpleMarkdownRules.createUnderlineRule(),
            nonterminal(
                ITALICS,
                // Only one of the two groups matches, depending on whether it's _ or *
                found -> found.group(1) != null ? 1 : 2,
                found -> new StyleNode<>(List.of(new TextStyle(TextStyle.Type.ITALICS)))
            ),
            SimpleMarkdownRules.createStrikethruRule(),
            SimpleMarkdownRules.createTextRule()
        ));
    }

    private static Node<Object> styled(MarkdownElement style, String text) {
        final var node = new StyleNode<Object, MarkdownElement>(List.of(style));
        node.addChild(new TextNode<>(text));
        return node;
    }

    private static Node<Object> mention(MarkdownElement.MentionKind kind, Matcher found) {
        return new StyleNode<>(List.of(new MarkdownElement.Mention(kind, found.group(1))));
    }

    private static Node<Object> link(String url) {
        return styled(new MarkdownElement.Link(url), url);
    }

    /**
     * A rule whose node has nothing inside it to parse.
     */
    private static Rule<Object, Node<Object>, Object> terminal(Pattern pattern, Function<Matcher, Node<Object>> node) {
        return new PatternRule(pattern, false, found -> ParseSpec.createTerminal(node.apply(found), null));
    }

    /**
     * A rule whose node has the text in the given group inside it, which is parsed too, so e.g. a
     * spoiler can have bold text inside it.
     */
    private static Rule<Object, Node<Object>, Object> nonterminal(
        Pattern pattern,
        int group,
        Function<Matcher, Node<Object>> node
    ) {
        return nonterminal(pattern, found -> group, node);
    }

    private static Rule<Object, Node<Object>, Object> nonterminal(
        Pattern pattern,
        Function<Matcher, Integer> group,
        Function<Matcher, Node<Object>> node
    ) {
        return new PatternRule(pattern, false, found -> inside(found, group.apply(found), node.apply(found)));
    }

    /**
     * A {@link #nonterminal} rule that only matches at the start of a line.
     */
    private static Rule<Object, Node<Object>, Object> lineStart(
        Pattern pattern,
        int group,
        Function<Matcher, Node<Object>> node
    ) {
        return new PatternRule(pattern, true, found -> inside(found, group, node.apply(found)));
    }

    /**
     * Parses the text in the group as the node's children. The group's position is relative to
     * the text the rule matched, which SimpleAST adjusts for.
     */
    private static ParseSpec<Object, Node<Object>, Object> inside(Matcher found, int group, Node<Object> node) {
        return ParseSpec.createNonterminal(node, null, found.start(group), found.end(group));
    }

    /**
     * A rule that makes a node from its pattern's match.
     */
    private static final class PatternRule extends Rule<Object, Node<Object>, Object> {
        private final boolean lineStart;
        private final Function<Matcher, ParseSpec<Object, Node<Object>, Object>> parse;

        PatternRule(
            Pattern pattern,
            boolean lineStart,
            Function<Matcher, ParseSpec<Object, Node<Object>, Object>> parse
        ) {
            super(pattern);
            this.lineStart = lineStart;
            this.parse = parse;
        }

        /**
         * Rules for the start of a line only match if nothing's been matched yet, or the last
         * thing matched ended a line.
         */
        @Override
        public Matcher match(CharSequence source, String lastCapture, Object state) {
            if (lineStart && lastCapture != null && !lastCapture.endsWith("\n")) {
                return null;
            }
            return super.match(source, lastCapture, state);
        }

        @Override
        public ParseSpec<Object, Node<Object>, Object> parse(
            Matcher matcher,
            Parser<Object, Node<Object>, Object> parser,
            Object state
        ) {
            return parse.apply(matcher);
        }
    }
}
